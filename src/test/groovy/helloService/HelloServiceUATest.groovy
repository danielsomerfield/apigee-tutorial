package helloService

import groovy.json.JsonSlurper
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.HttpClients
import org.junit.Test

import static org.junit.Assert.*

class HelloServiceUATest {

    @Test
    def void testHelloServiceEndpoint() {
        assertEquals(
                "Hello, Daniel!",
                doGetRequest("/hello?name=Daniel").message
        )
    }

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
