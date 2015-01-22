package testUtils

import groovy.json.JsonSlurper
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.ProtocolException
import org.apache.http.client.RedirectStrategy
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.HttpClients
import org.apache.http.protocol.HttpContext

import static testUtils.HttpBuilder.RequestFactory.get
import static testUtils.HttpBuilder.Scheme.http

public class HttpBuilder {

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

    Scheme scheme = System.getenv("SERVICE_SCHEME") == null ? http : Scheme.valueOf(System.getenv("SERVICE_PORT"))
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

    private class Response {

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
            closure(new Response(exec()))
        }
    }
}
