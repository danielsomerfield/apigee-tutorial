---
title: Write Your Service
layout: tutorial
tag: write-your-service
---
---
## ***IN PROGRESS*** ##

---

We've set up the gradle project in [section 1](1-setup-build-scripts.html) and written an acceptance test in [section 2](2-write-your-acceptance-test.html). Now we being implementation.

Honestly, I did this part in a couple of steps. First I did a first cut with everything crammed into the main class but it left some rather difficult to test code, so I broke it into a three concerns: the main runtime entry point, the web request/response handling code, and the actual service logic. This makes the code considerably more testable--it seems a little overkill with a service that does so little, but now I can easily write unit tests for my business logic.

Starting from the main runtime, we have this simple class:
{% highlight groovy %}
package helloService

public class HelloServiceMain {

  public static void main(String[] args) {
    new HelloServer(port: (System.getenv("helloService.port") ?: "8080").toInteger()).start()
  }
}
{% endhighlight %}
Nothing really here, but creating and starting up the server class. I have added an runtime property that allows you to override the port the server runs on, but defaulted to 8080. There's a little more going on in the server class:
{% highlight groovy %}
package helloService

import groovy.json.JsonBuilder
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.HandlerCollection

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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
This is the real meat of the code at this point. It's all the logic required to configure and start up the embedded jetty server that is going to be handling the requests. *If you are curious why I chose to use embedded jetty for this code, see [Why Embedded Jetty](#embedded-jetty) below.*

***TODO: write description of above class****

<h4>Why Embedded Jetty?</h4>
<a name="embedded-jetty"></a>
Why did I chose to use an embedded webserver, rather than deploy a war into a container. And why jetty?

Much of this is personal predisposition and familiarity, but
***TODO: finish this***
