package helloService

import groovy.json.JsonBuilder
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.HandlerCollection

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

public class HelloServiceMain {

    public static void main(String[] args) {
        new HelloService(port: (System.getenv("PORT") ?: "8080").toInteger()).start()
    }
}