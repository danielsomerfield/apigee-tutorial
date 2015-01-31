package helloService

import groovy.json.JsonBuilder
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.HandlerCollection

import javax.servlet.ServletException
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
        addHandlerWithContext(collection, "/ping", createPingHandler());
        if (sslOnly()) {
            collection.addHandler(createSSLCheckHandler())
        }
        addHandlerWithContext(collection, "/hello", createHelloHandler())
        return collection;
    }

    static def Handler createSSLCheckHandler() {
        { final String target, Request baseRequest,
          final HttpServletRequest request,
          final HttpServletResponse response ->
            if (!baseRequest.isHandled() && !"https".equals(request.getHeader("X-Forwarded-Proto"))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Nice try, wiseguy.")
                baseRequest.setHandled(true)
            }
        } as AbstractHandler
    }

    static def sslOnly() {
        System.getenv("HELLO_SERVER_SSL_DISABLED") != "true"
    }

    def static addHandlerWithContext(HandlerCollection collection, String context, Handler handler) {
        ContextHandler contextHandler = new ContextHandler(context)
        contextHandler.setHandler(handler)
        collection.addHandler(contextHandler)
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

    def static createPingHandler() {
        { final String target, Request baseRequest,
          final HttpServletRequest request,
          final HttpServletResponse response ->
            response.setContentType("application/json")
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
