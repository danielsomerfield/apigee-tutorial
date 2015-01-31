package helloService

import groovy.json.JsonBuilder
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.HandlerCollection
import org.slf4j.LoggerFactory

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class HelloServer {

    def static logger = LoggerFactory.getLogger(HelloServer.class)

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
