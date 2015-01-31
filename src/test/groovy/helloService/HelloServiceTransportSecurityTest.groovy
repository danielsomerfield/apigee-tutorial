package helloService
import org.junit.Test
import testUtils.HttpBuilder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static testUtils.HttpBuilder.Scheme.http
import static testUtils.HttpBuilder.Scheme.https
import static testUtils.HttpBuilder.serviceClient

class HelloServiceTransportSecurityTest {

    @Test
    public void testServiceIsAvailableViaSSL() {
        serviceClient {
            path = "/hello/?name=Daniel"
            port = 443
            scheme = https
        }.execute().then {
            assertEquals(200, it.get().httpStatus)
        }
    }

    @Test
    public void testServiceIsNotAvailableWithoutSSL() {
        serviceClient {
            path = "/hello/?name=Daniel"
            port = 80
            scheme = http
        }.execute().then { Optional<HttpBuilder.Response> response->
            assertFalse(response.isPresent());
        }
    }

}
