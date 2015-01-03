---
title: Write Your Service
layout: tutorial
tag: write-your-service-2
---
We've set up the gradle project in [section 1](1-setup-build-scripts.html) and written an acceptance test in [section 2](2-write-your-acceptance-test.html). Now we being implementation.

Honestly, I did this part in a couple of steps. First I did a first cut with everything crammed into the main class but it left some rather difficult to test code, so I broke it into a three concerns: the main runtime entry point, the web request/response handling code, and the actual service logic. This makes the code considerably more testable--it seems a little overkill with a service that does so little, but now I can easily write unit tests for my business logic.

Starting from the main runtime, we have this simple class:
{% highlight groovy %}
package helloService

public class HelloServiceMain {

  public static void main(String[] args) {
    new HelloServer(port: (System.getenv("HELLO_SERVICE_PORT") ?: "8080").toInteger()).start()
  }
}
{% endhighlight %}
[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/write-your-service/src/main/groovy/helloService/HelloServiceMain.groovy)

Nothing really here, but creating and starting up the server class. I have added an runtime property that allows you to override the port the server runs on, but defaulted to 8080. There's a little more going on in the server class:
{% highlight groovy %}
package helloService

class HelloServer {

  def int port
  def HelloService helloService = new HelloService();

  def start() {
    def server = new Server(port)
    server.setHandler(getHandlers())
    server.start()
  }

  def HandlerCollection getHandlers() {
    HandlerCollection collection = new HandlerCollection();
    ContextHandler context = new ContextHandler("/hello")
    context.setHandler(createHelloHandler())
    collection.addHandler(context)
    return collection;
  }

  def createHelloHandler() {
    { final String target, Request baseRequest,
      final HttpServletRequest request,
      final HttpServletResponse response ->
      response.setContentType("application/json")
      response.getWriter().withCloseable { writer ->
        writer.println(buildJSON(request.getParameter("name")))
      }
      baseRequest.setHandled(true)
    } as AbstractHandler
  }

  def buildJSON(String name) {
    def struct = [
    message: helloService.sayHello(name)
    ]
    new JsonBuilder(struct).toPrettyString()
  }
}
{% endhighlight %}
[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/write-your-service/src/main/groovy/helloService/HelloServer.groovy)

This is the real meat of the code at this point. It's all the logic required to configure and start up the embedded jetty server that is going to be handling the requests. *If you are curious why I chose to use embedded jetty for this code, see [Sidebar: Why Embedded Jetty](#embedded-jetty) below.*

The key, and possibly somewhat obscure part of the code is the ```createHelloHandler()``` method. I use a little groovy syntactic sugar to turn a closure into an abstract class. Bottom line is that this code creates the ```Handler``` which, in Jetty terms, is the class responsible for handling application request. This is a pretty naive and sloppy implementation so perhaps we'll need to improve it later. The path to which the handler is bound is set by the `ContextHandler`. Again, this is pretty rugged: no checks for incoming content-type, no check for HTTP method--all the things to tighten the focus of the request handling. Perhaps we'll improve that later by writing some tests!

You'll notce that there is no real business logic in this class--just handling and packaging. The business logic (such as it is) has been delegated to the HelloService class:
{% highlight groovy %}
package helloService

class HelloService {
  def String sayHello(String name) {
    "Hello, $name!"
  }
}
{% endhighlight %}
[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/write-your-service/src/main/groovy/helloService/HelloService.groovy)

And, of course, we have written a basic unit test to assert expected behavior.

{% highlight groovy %}
package helloService

class HelloServiceTest {

  @Test
  def void testSayHello() {
    Assert.assertEquals(
      "Hello, Daniel!",
      new HelloService().sayHello("Daniel")
    )
  }
}
{% endhighlight %}

[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/write-your-service/src/test/groovy/helloService/HelloServiceTest.groovy)

With that, we have a full implementation. It's not really packaged. It's not in our delivery pipeline, but it should work.

So, let's try it. Our automation is a little clumsy at this point, so we have to run our server and user acceptance tests separately. So first, start up the server:
{% highlight bash %}
╰─➤  ./gradlew run                                                                                                    1 ↵
:compileJava UP-TO-DATE
:compileGroovy UP-TO-DATE
:processResources UP-TO-DATE
:classes UP-TO-DATE
:run
2015-01-03 08:28:08.936:INFO:oejs.Server:jetty-8.1.16.v20140903
2015-01-03 08:28:08.971:INFO:oejs.AbstractConnector:Started SelectChannelConnector@0.0.0.0:8080
> Building 80% > :run
{% endhighlight %}
Then, in a separate shell, run the user acceptance tests.
{% highlight bash %}
╰─➤  ./gradlew uat                                                                                                  130 ↵
:compileJava UP-TO-DATE
:compileGroovy UP-TO-DATE
:processResources UP-TO-DATE
:classes UP-TO-DATE
:compileTestJava UP-TO-DATE
:compileTestGroovy
:processTestResources UP-TO-DATE
:testClasses
:test
:uat

BUILD SUCCESSFUL

Total time: 5.581 secs
{% endhighlight %}
Booyah! It worked! Might not be the worst thing in the world to manually validate, to make sure that you don't have a bug in your tests. You can do that with curl or with a browser:
{% highlight bash %}
╰─➤  curl -v "http://localhost:8080/hello?name=Zeigfreid"
* Hostname was NOT found in DNS cache
*   Trying ::1...
* Connected to localhost (::1) port 8080 (#0)
> GET /hello?name=Zeigfreid HTTP/1.1
> User-Agent: curl/7.37.1
> Host: localhost:8080
> Accept: */*
>
< HTTP/1.1 302 Found
< Location: http://localhost:8080/hello/?name=Zeigfreid
< Content-Length: 0
* Server Jetty(8.1.16.v20140903) is not blacklisted
< Server: Jetty(8.1.16.v20140903)
<
* Connection #0 to host localhost left intact
{% endhighlight %}
Huh. Not what I expected. The reason is that jetty has a little voodoo that checks the path (`/hello`) and notices that there is no additional info beyond the context. In this case, by default, it redirects to the path as a directory name. Fortunately (or unfortunately) the apache http client we are using in our tests transparently does the redirect for us and it *just works*. At this point, I would probably recommend disabling the automatic redirect following feature of the client and then you an decide exactly what you expect the behavior to be. You don't necessarily know what client is going to be calling your service and your best strategy is to make very specific decisions about the contract.

So let's do that:

After disabling the automatic redirect in the user acceptance test, it fails:

Then, in a separate shell, run the user acceptance tests.
{% highlight bash %}
╰─➤  ./gradlew uat
:compileJava UP-TO-DATE
:compileGroovy UP-TO-DATE
:processResources UP-TO-DATE
:classes UP-TO-DATE
:compileTestJava UP-TO-DATE
:compileTestGroovy
:processTestResources UP-TO-DATE
:testClasses
:test
:uat

helloService.HelloServiceUATest > testHelloServiceEndpoint FAILED
groovy.json.JsonException at HelloServiceUATest.groovy:11

1 test completed, 1 failed
:uat FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':uat'.
> There were failing tests. See the report at: file:///Users/danielsomerfield/tw_workspace/apigee_tutorial/build/reports/tests/index.html

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

BUILD FAILED

Total time: 5.618 secs
{% endhighlight %}

If you look at why, in the report tests, you'll find the JSON won't parse, presumably because it isn't really JSON. This highlights two things:

- we need to make a decision about our behavior
- we need more detail in our UA test.

I have decided that I am fine with that behavior, but I think I should codify it in the test by checking the response code if there is no trailing slash, and then checking both response code and content if there is. I have to tweak my HttpUtils class a little, as well as my test, giving me this:

{% highlight groovy %}
class HelloServiceUATest {
  @Test
  def void testHelloServiceEndpoint() {
    doGetRequest("/hello/?name=Daniel") {
      assertEquals(200, it.httpStatus)
      assertEquals(
        "Hello, Daniel!",
        it.json.message
      )
    }
  }

  @Test
  def void testHelloServiceEndpointRedirect() {
    doGetRequest("/hello?name=Daniel") {
      assertEquals(302, it.httpStatus)
    }
  }
}
{% endhighlight %}
[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/write-your-service-2/src/test/groovy/helloService/HelloServiceTest.groovy)

And

{% highlight groovy %}
package helloService

class HttpUtils {

  private static final NEVER_REDIRECT_STRATEGY = new RedirectStrategy() {
    @Override
    boolean isRedirected(
      final HttpRequest req,
      final HttpResponse res, final HttpContext context) throws ProtocolException {
        false
      }

      @Override
      HttpUriRequest getRedirect(
        final HttpRequest request,
        final HttpResponse response, final HttpContext context) throws ProtocolException {
          throw new UnsupportedOperationException("This code should never run")
        }
      }

      def static doGetRequest(String path, Closure then = {}) {
        doHttpRequestLazy(new HttpGet(pathToURL(path)), then)
      }

      def static doHttpRequestLazy(HttpUriRequest request, Closure then) {
        HttpClients.custom().setRedirectStrategy(NEVER_REDIRECT_STRATEGY)
        .build().withCloseable() { client ->
          client.execute(request).withCloseable() { response ->
            then(new HttpTestResponse(response : response))
          }
        }
      }

      static class HttpTestResponse {

        def CloseableHttpResponse response

        def getJson() {
          response.getEntity().content.withCloseable { i ->
            return new JsonSlurper().parse(new InputStreamReader(i))
          }
        }

        def int getHttpStatus() {response.statusLine.statusCode}
      }


      def static pathToURL(String path){
        String urlRoot = System.getProperty("HELLO_SERVICE_ROOT") ?: "http://localhost:8080"
        return "$urlRoot$path"
      }
{% endhighlight %}
[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/write-your-service-2/src/test/groovy/helloService/HttpUtils.groovy)

I made my HttpUtils class a bit uglier but kept my tests clear and clean. I believe that should be the priority. In addition to providing regression, your tests are your most important form of documentation. Specs are often wrong. User docs are almost always wrong. Tests--if they're passing--are right insofar as they validate behavior.

So run it again and all is well!
{% highlight bash %}
╰─➤  ./gradlew uat
:compileJava UP-TO-DATE
:compileGroovy UP-TO-DATE
:processResources UP-TO-DATE
:classes UP-TO-DATE
:compileTestJava UP-TO-DATE
:compileTestGroovy UP-TO-DATE
:processTestResources UP-TO-DATE
:testClasses UP-TO-DATE
:test
:uat

BUILD SUCCESSFUL

Total time: 4.325 secs
{% endhighlight %}

Now, when we provide our API, we have to note that:
- a request to `hello/?name=Daniel` will return a 200
- a request to `hello?name=Daniel` will return a 302 and clients should behave accordingly.

So now we have a functioning, testing service. So now what? Works great locally, but now I want to get it to work in my continuous delivery pipeline.

On to CD!!!

## [Continue to "Section 4: Setting up Snap CI"](4-setting-up-snap-ci.html) ##


<h4>Sidebar: Why Embedded Jetty?</h4>
<a name="embedded-jetty"></a>
Why did I chose to use an embedded webserver, rather than deploy a war into a container. And why jetty?

Much of this is personal predisposition and familiarity, but I find that I like a simple service to be a stand-alone application. It reduces the packing overhead a bit, and deploys more broadly. There are definitely pros and cons, however. Some cloud service will expect you to deploy in a web container, like tomcat or jetty. Converting to the servlet API would not be much work, but it would need to be done. I also don't love the Servlet API, which leads to the answer to the next question: why jetty?

In my experience, jetty embeds easily with a nice API for binding parameters. It tends to be better supported in tools. If I am building a full blown website, I'd be inclined to use something with a nicer API for binding routes and the like, but for a simple service, I find Jetty about perfect. I have read performance tests that indicate that tomcat scales better under high load, so you'd want to do some research and testing before you make your platform decision, but for a tutorial like this, jetty is about perfect.
