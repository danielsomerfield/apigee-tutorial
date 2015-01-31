package helloService

import org.hamcrest.CustomMatcher
import org.hamcrest.Matcher
import org.junit.Test
import testUtils.HttpBuilder

import java.util.function.Consumer

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
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
            response.ifPresent(new Consumer<HttpBuilder.Response>() {
                @Override
                void accept(final HttpBuilder.Response r) {
                    assertThat(r.getHttpStatus(), isErrorCode())
                }

                Matcher<? super Integer> isErrorCode() {
                    return new CustomMatcher<Integer>() {
                        @Override
                        boolean matches(final Object item) {
                            return item instanceof Integer && item > 299
                        }
                    }
                }
            })
        }
    }


}
