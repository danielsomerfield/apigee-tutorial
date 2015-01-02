package helloService

import org.junit.Assert
import org.junit.Test

class HelloServiceTest {

    @Test
    def void testSayHello() {
        Assert.assertEquals(
                "Hello, Daniel!",
                new HelloService().sayHello("Daniel")
        )
    }
}
