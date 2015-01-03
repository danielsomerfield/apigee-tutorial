package helloService

import org.junit.Test

import static helloService.HttpUtils.doGetRequest
import static org.junit.Assert.assertEquals

class HelloServiceUATest {
    @Test
    def void testHelloServiceEndpoint() {
        doGetRequest("/hello/?name=Daniel") {
            assertEquals(200, it.httpStatus)
            assertEquals(
                    "Hello, Daniel!",
                    it.json.message
            )
        }
    }

    @Test
    def void testHelloServiceEndpointRedirect() {
        doGetRequest("/hello?name=Daniel") {
            assertEquals(302, it.httpStatus)
        }
    }
}
