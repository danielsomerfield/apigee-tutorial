import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.gradle.api.GradleException

def static waitUntil(long maxWait, Closure closure) {
    long timeout = System.currentTimeMillis() + maxWait;

    while (System.currentTimeMillis() < timeout) {
        if (closure() == true) {
            return
        }
        Thread.sleep(500)
    }
    throw new GradleException("Wait for expected condition timed out.")
}

def static ping(String url) {
    try {
        HttpGet get = new HttpGet(url)
        HttpClients.createDefault().withCloseable() { client ->
            client.execute(get).withCloseable() { response ->
                return response.statusLine.statusCode == 200
            }
        }
    }
    catch (IOException ignored) {
        return false;
    }
}
