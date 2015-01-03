package helloService
import groovy.json.JsonSlurper
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.ProtocolException
import org.apache.http.client.RedirectStrategy
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.HttpClients
import org.apache.http.protocol.HttpContext

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
}
