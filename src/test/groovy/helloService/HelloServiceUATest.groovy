package helloService
import org.junit.Test

import static org.junit.Assert.assertEquals
import static testUtils.HttpBuilder.serviceClient

class HelloServiceUATest {

    @Test
    def void testHelloServiceEndpoint() {
        serviceClient() {
            path = "/hello/?name=Daniel"
        }.execute().then {
            assertEquals(200, it.get().httpStatus)
            assertEquals(
                    "Hello, Daniel!",
                    it.get().json.message
            )
        }
    }

    @Test
    def void testHelloServiceEndpointRedirect() {
        serviceClient() {
            path = "/hello?name=Daniel"
        }.execute().then {
            assertEquals(302, it.get().httpStatus)
        }
    }
}
