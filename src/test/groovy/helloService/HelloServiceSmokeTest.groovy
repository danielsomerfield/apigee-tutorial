package helloService

import org.junit.runner.RunWith
import org.junit.runners.Suite
import static org.junit.runners.Suite.SuiteClasses

@RunWith(Suite.class)
@SuiteClasses([HelloServiceUATest.class])
class HelloServiceSmokeTest {

}
