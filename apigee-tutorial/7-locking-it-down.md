---
title: Apigee Deployment
layout: tutorial
tag: apigee-deployment
---
In [the last section](6-apigee-deployment.html), we added Apigee to our pipeline. Now we can work with that full stack pipeline to add cool features to our product.

When we configured the final step in our pipeline, we ran our smoke test again the url `http://`*myhost*`-prod.apigee.net` but our application is in the clear. There is nothing to prevent snoops and hackers from stealing our precious data. So here's what we want to do:

- Secure the transport between the client and Apigee
- Secure the transport between Apigee and the Heroku application
- Lock down application access to our target users

One note on securing services: do not deploy an application with sensitive security needs to a public host, then lock it down. You don't ever want to leave it susceptible to attack. If you need to test your security mechanism, roll out "hello world" over your pipeline, write tests that assert the security requirements, then roll out your actual application endpoint with the security mechanism in place. That way your code will be well-tested, you will be practicing good continuous delivery by delivering MVPs, but you won't leave yourself open to an attack while you build up the security.

## Securing the Transport
When we configured the proxy bundle, we used the default virtual host, which means our connection is over http. Luckily this is **really** easy to fix. If we never want our tests to fail, we would have to deploy a second host, but, personally, I'd rather update the assertion in the tests, let it fail, then deploy the fix.

I want to assert two things in my test:

- I can connect to the endpoint over SSL
- I can't connect to the endpoint if I'm NOT connecting over SSL

This means we need to re-work our tests a little bit. In fact, it's really time for a refactor. The code has gotten a bit gross, particularly around the HTTP client classes. So we should fix them. We've coded ourselves into a bit of a corner with the HTTP Client code which will make it hard to write the security tests. It's also just unpleasant code, so we should make the API a bit nice. In this case a builder with rational defaults and an override seems like a nice way to go:
{% highlight groovy %}
...
public class HttpBuilder {
...
    enum Scheme {
        http, https
    }

    enum RequestFactory {
        get {
            public HttpRequestBase create(URI uri){
                new HttpGet(uri)
            }
        }
        public abstract HttpRequestBase create(URI uri)
    }

    Scheme scheme = System.getenv("SERVICE_SCHEME") == null ? http : Scheme.valueOf(System.getenv("SERVICE_SCHEME"))
    String host = System.getenv("SERVICE_HOST") ?: "localhost"
    int port = Integer.parseInt(System.getenv("SERVICE_PORT") ?: "8080")
    String path = "/"
    RequestFactory method = get

    public static def HttpBuilder serviceClient(Closure configClosure){
        def builder = new HttpBuilder()
        configClosure.delegate = builder;
        configClosure()
        return builder;
    }

    public uri() {
        URI.create("$scheme://$host:$port$path")
    }

    def execute() {
        new HttpExecution();
    }

    public class Response {

        private HttpResponse response;

        Response(HttpResponse response) {
            this.response = response
        }

        def int getHttpStatus() {
            response.statusLine.statusCode
        }

        def getJson() {
            response.getEntity().content.withCloseable { i ->
                return new JsonSlurper().parse(new InputStreamReader(i))
            }
        }
    }

    private class HttpExecution {
        private def exec() {
            HttpClients.custom().setRedirectStrategy(NEVER_REDIRECT_STRATEGY).build().execute(method.create(uri()))
        }
        def then(Closure closure) {
            try {
                closure(Optional.of(new Response(exec())))
            } catch (Exception e) {
                e.printStackTrace()
                closure(Optional.empty())
            }
        }
    }
}

{% endhighlight %}
*[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/locking-it-down/src/test/groovy/testUtils/HttpBuilder.groovy)*

The builder pattern gives us a nice api for our client and groovy allows us to use the the configuration closure mechanism to make the API easy to use. By setting the `delegate` on the closure, all calls within the closure will resolve to the target object, in this case our builder instance itself. That allows us to do things like this:

{% highlight groovy %}
...
class HelloServiceTransportSecuritySmokeTest {

    @Test
    public void testServiceIsAvailableViaSSL() {
        serviceClient {
            path = "/hello/?name=Daniel"
            port = 443
            scheme = https
        }.execute().then {
            assertEquals(200, it.get().httpStatus)
        }
    }

    @Test
    public void testServiceIsNotAvailableWithoutSSL() {
        serviceClient {
            path = "/hello/?name=Daniel"
            port = 80
            scheme = http
        }.execute().then { Optional<HttpBuilder.Response> response->
            response.ifPresent(new Consumer<HttpBuilder.Response>() {
                @Override
                void accept(final HttpBuilder.Response r) {
                    assertThat(r.getHttpStatus(), isErrorCode())
                }

                Matcher<? super Integer> isErrorCode() {
                    return new CustomMatcher<Integer>("an HTTP error") {
                        @Override
                        boolean matches(final Object item) {
                            return item instanceof Integer && item > 299
                        }
                    }
                }
            })
        }
    }
}
{% endhighlight %}
*[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/locking-it-down/src/test/groovy/helloService/HelloServiceTransportSecuritySmokeTest.groovy)*

These are our two new tests. One asserts that you can hit the application over https on port 443. The other asserts that you can't hit over http on port 80. This is a nice regression in case someone decides to flip on the non-SSL endpoint. Of course, at this point, the tests will fail since we aren't running over SSL, but we'll fix that shortly.

First we need to tweak our pipeline to reflect our new SSL reality. Let's change our SMOKE and APIGEE-SMOKE TEST configuration to have the following environmental variable values:

- SERVICE_SCHEME:  https
- SERVICE_PORT:  443
- SERVICE_HOST: ***host-name***.herokuapp.com

We're going to have to change the `BuildUtils.groovy` a little bit so we don't have to use the new environment configuration.

{% highlight groovy %}
...
def static pingUrl() {
    return "${getEnv('SERVICE_SCHEME', 'http')}://${getEnv('SERVICE_HOST', 'localhost')}:${getEnv('SERVICE_PORT', '8080')}/ping"
}

def static ping() {
    try {
        HttpGet get = new HttpGet(pingUrl())
        HttpClients.createDefault().withCloseable() { client ->
            client.execute(get).withCloseable() { response ->
                return response.statusLine.statusCode == 200
            }
        }
    }
    catch (IOException ignored) {
        return false;
    }
}

def static String getEnv(String name, String defaultValue) {
    def value =  System.getenv(name);
    return value ?: defaultValue;
}

def static waitForPing(long timeInMillis) {

    logger.info("waiting for $timeInMillis milliseconds until ping to ${pingUrl()} returns 200")
    waitUntil(timeInMillis) {
        ping()
    }
}
...
{% endhighlight %}
*[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/locking-it-down/buildSrc/src/main/groovy/BuildUtils.groovy)*

If we push those changes, our tests will fail because, of course, we aren't enabling SSL or disabling non-SSL connections to Heroku. In fact, if somehow they got past the first smoke test, the build would fail on the second because the connection to Apigee is not secured either. So we're going to have to secure the connection to both.

## Securing the Transport to Heroku
First we want to guarantee the at the connection to Heroku is over SSL. If you look at the test failure, you will notice that `testServiceIsAvailableViaSSL` succeeded, but `testServiceIsNotAvailableWithoutSSL` fails. This is because, out of the box, Heroku provide both a secure and unsecure endpoint. So we don't need to do anything to secure the endpoint, we just need to prevent connections via the unsecure endpoint. As far as I know, there is no way to disable the non-SSL endpoint, but it *does* provide us with a mechanism to ensure that the connection is via SSL via the `X-Forwarded-Proto` header. This value will contain "http" or "https" depending on the connection. Now this does not mean that connection to our app will technical be over SSL, so theoretically someone within the Heroku network (like a Heroku network admin) might be able to snoop network connections, however, it does mean that the external interface will be protected. Given that our application simply says hello, we'll call this good enough.

We'll handle the security check by adding a handler that fails if the expected variable is not there. We'll also add the ability to disable it so we don't need to set up SSL infrastructure just to test the application locally. The default, however, will be to require SSL.

{% highlight groovy %}

def HandlerCollection getHandlers() {
    HandlerCollection collection = new HandlerCollection();
    addHandlerWithContext(collection, "/ping", createPingHandler());
    if (sslOnly()) {
        println("*********************")
        println("The server is running in SSL-only mode!!!!!!!!!")
        println("*********************")
        logger.info("The server is running in SSL-only mode.")
        collection.addHandler(createSSLCheckHandler())
    } else {
        logger.warn("The server is not running in SSL-only mode so connections on non-secure ports is currently allowed.")
    }
    addHandlerWithContext(collection, "/hello", createHelloHandler())
    return collection;
}

static def Handler createSSLCheckHandler() {
    { final String target, Request baseRequest,
      final HttpServletRequest request,
      final HttpServletResponse response ->
        if (!baseRequest.isHandled() && !"https".equals(request.getHeader("X-Forwarded-Proto"))) {
            logger.warn("Request denied on non-SSL port.")
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Nice try, wiseguy.")
            baseRequest.setHandled(true)
        } else {
            logger.warn("Request allowed on a non-SSL port. You must be allowing non SSL connections.")
        }
    } as AbstractHandler
}

static def sslOnly() {
    System.getenv("HELLO_SERVER_SSL_DISABLED") != "true"
}

{% endhighlight %}
*[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/locking-it-down/src/main/groovy/helloService/HelloServer.groovy)*

Now, if you want to run the UA tests on your local machine, you will need to set the "HELLO_SERVER_SSL_DISABLED" environment variable to 'true'.

After committing this change, our first smoke test should pass, but it still fails on the second run--in fact both tests fail. Why is that? Unlike Heroku which comes with SSL out of the box, we need to specifically configure Apigee to use SSL. We do that by changing one word in the proxy configuration file at `api-proxy-bundle/src/main/config/apiproxy/proxies/default.xml` and one in the target configuration at `api-proxy-bundle/src/main/config/apiproxy/targets/default.xml`

{% highlight xml %}
<ProxyEndpoint name="default">
    <HTTPProxyConnection>
        <BasePath>/</BasePath>
        <VirtualHost>secure</VirtualHost>
    </HTTPProxyConnection>
    <RouteRule name="default">
        <TargetEndpoint>default</TargetEndpoint>
    </RouteRule>
</ProxyEndpoint>
{% endhighlight %}
*[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/locaking-it-down/api-proxy-bundle/src/main/config/apiproxy/proxies/default.xml)*

{% highlight xml %}
<TargetEndpoint name="default">
    <Description>Target for the apigee CD tutorial</Description>
    <FaultRules/>
    <Flows/>
    <HTTPTargetConnection>
        <Properties/>
        <URL>https://apigee-tutorial.herokuapp.com/</URL>
    </HTTPTargetConnection>
</TargetEndpoint>
{% endhighlight %}
*[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/locaking-it-down/api-proxy-bundle/src/main/config/apiproxy/targets/default.xml)*


In the `VirtualHost` tag, changing "default" to "secure" enables. The targets file just involves replacing the "http" with "https" in the URL tag. Simple. Now, if you commit and run, you should be seeing green!

Great. Now our fantastic app is protected from snoopers... of course it's wide open for anyone to use. So let's lock it down more. In the next section we're going to use Apigee's "last mile" security to ensure requests to the app are going through Apigee. Then we're going to use Apigee's oauth implementation to make sure our users are registered.

## [Continue to "Section 8: Locking it Down Part 2"](8-locking-it-down-2.html) ##



- Changes
- Added tests for validating ssl behavior
- Added check for ssl behavior
- Added env variable to first smoketest in pipeline to disable ssl
- Changed ports in heroku and apigee pipeline steps
- When things went wrong:
  - Heroku was pulling the wrong version
  - Fixed when blowing away app and local .m2
