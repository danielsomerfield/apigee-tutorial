---
title: Write Your Acceptance Tests
layout: tutorial
tag: write-your-service
---
In the [first section](1-setup-build-scripts.html), we set up our preliminary build environment. Now we move on to code.

I think there is value in codifying the desired result of this tutorial into an acceptance test. It keeps us honestly. It provides a mechanism for regression testing. If you are a BDD or TDD person, you don't need to be convinced, but if you're not, well, you're not. In that case, just be happy that while you go through the tutorial you have these tests to make sure you have everything set up right.

If I was going to be really ambitious I would set up the project to run cucumber tests for the User Acceptance tests, but in this case, it would just complicate the project, so I'm just going to use vanilla junit tests written in groovy.

Our first acceptance tests will simply verify that our service is up and running and responding as expected. I'm going to look ahead a little bit to our forthcoming Continuous Integration scenario and write the tests so they run either locally or in a pipeline.

#### Testing Requirements ####
So let's start with the application requirements:
As a user of this system, I would like to be able to send an GET request to /hello endpoint with a name parameter and get back a friendly message in a JSON payload as follows:

***Input***

> GET /hello?Daniel

***Output***
{% highlight json %}
{
  message:"Hello, Daniel!"
}
{% endhighlight %}

So how does that look like in code?

{% highlight groovy %}
package helloService

class HelloServiceUATest {
  @Test
  def void testHelloServiceEndpoint() {
    def jsonResult = doGetRequest("/hello?name=Daniel")
    assertEquals(
      "Hello, Daniel!",
      jsonResult.message
    )
  }
}
{% endhighlight %}
[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/write-your-service/src/test/groovy/helloService/HelloServiceTest.groovy)

Again, pretty standard stuff. A lot of the heavy lifting occurs in the HttpUtils class, shown below. The test itself is extremely simple: assert that, given the input we can make the HTTP request and receive the expected output. The only thing that might look a bit odd is the ".message" call on the result of jsonResult. This is some groovy JSON / dynamic binding magic. The JSON library returns a map representing the JSON. Groovy turns ".message" into a call to Map.get() call. It allows

The utility class, for the sake of completeness, is as follows:
{% highlight groovy %}
package helloService

class HttpUtils {

  def static doGetRequest(String path) {
    def HttpGet get = new HttpGet(pathToURL(path));
    doHttpRequest(get)
  }

  def static doHttpRequest(HttpUriRequest request) {
    HttpClients.custom()
    .build().withCloseable() { client ->
      CloseableHttpResponse response = client.execute(request)
      response.getEntity().content.withCloseable { i ->
        return new JsonSlurper().parse(new InputStreamReader(i))
      }
    }
  }

  def static pathToURL(String path){
    String urlRoot = System.getProperty("HELLO_SERVICE_ROOT") ?: "http://localhost:8080"
    return "$urlRoot$path"
  }
}
{% endhighlight %}
[View the full file on GitHub](https://github.com/danielsomerfield/apigee-tutorial/blob/write-your-service/src/test/groovy/helloService/HttpUtils.groovy)

We'll use this class again and possibly refactor it a bit on the way. The important thing to observe is that the pathToURL function allows overrides so we can run the tests against another target if we choose to do so.

If you run the acceptance test target, you will see something like this:

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

helloService.HelloServiceUATest > testHelloServiceEndpoint FAILED
org.apache.http.conn.HttpHostConnectException at HelloServiceUATest.groovy:11
Caused by: java.net.ConnectException at HelloServiceUATest.groovy:11

1 test completed, 1 failed
:uat FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':uat'.
> There were failing tests. See the report at: file:apigee_tutorial/build/reports/tests/index.html

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

BUILD FAILED
{% endhighlight %}

And, if we look at the provided fail path, it will contain details, including a stacktrace:

{% highlight bash %}
org.apache.http.conn.HttpHostConnectException: Connect to localhost:8080 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] failed: Connection refused
at org.apache.http.impl.conn.HttpClientConnectionOperator.connect(HttpClientConnectionOperator.java:142)
at org.apache.http.impl.conn.PoolingHttpClientConnectionManager.connect(PoolingHttpClientConnectionManager.java:319)
at org.apache.http.impl.execchain.MainClientExec.establishRoute(MainClientExec.java:363)
at org.apache.http.impl.execchain.MainClientExec.execute(MainClientExec.java:219)
at org.apache.http.impl.execchain.ProtocolExec.execute(ProtocolExec.java:195)
...
{% endhighlight %}
Again, not terribly surprising since we didn't actually write any code to handle the connection. And so we will...

## [Continue to "Section 3: Write Your Service"](3-write-your-service.html) ##
