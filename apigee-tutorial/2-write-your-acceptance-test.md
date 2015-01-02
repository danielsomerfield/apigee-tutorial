---
title: Write Your Acceptance Tests
layout: tutorial
tag: write-your-service
---
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

import org.junit.Test

import static helloService.HttpUtils.doGetRequest
import static org.junit.Assert.assertEquals

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

Again, pretty standard stuff. A lot of the heavy lifting occurs in the HttpUtils class, shown below. The test itself is extremely simple: assert that, given the input we can make the HTTP request and receive the expected output. The only thing that might look a bit odd is the ".message" call on the result of jsonResult. This is some groovy JSON / dynamic binding magic. The JSON library returns a map representing the JSON. Groovy turns ".message" into a call to Map.get() call. It allows

The utility class, for the sake of completeness, is as follows:
{% highlight groovy %}
package helloService

import groovy.json.JsonSlurper
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.HttpClients

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
