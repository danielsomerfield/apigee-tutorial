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
