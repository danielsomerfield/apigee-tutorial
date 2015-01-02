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
