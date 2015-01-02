package helloService

public class HelloServiceMain {

    public static void main(String[] args) {
        new HelloServer(port: (System.getenv("PORT") ?: "8080").toInteger()).start()
    }
}