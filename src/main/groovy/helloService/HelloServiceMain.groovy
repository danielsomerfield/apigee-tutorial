package helloService

public class HelloServiceMain {

    public static void main(String[] args) {
        new HelloServer(port: (System.getenv("HELLO_SERVICE_PORT") ?: "8080").toInteger()).start()
    }
}