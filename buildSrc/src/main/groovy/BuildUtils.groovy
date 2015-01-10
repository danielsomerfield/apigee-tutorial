import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger

def static generateVersion() {
    def versionNumber = '1.0.0'
    def buildName = System.getenv("CI") == 'true' ? System.getenv("SNAP_PIPELINE_COUNTER") : "DEV"
    "$versionNumber-$buildName"
}

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

def static waitForPing() {
    def url = System.getProperty("HELLO_SERVICE_ROOT") ?: "http://localhost:8080";
    waitUntil(5000) {
        ping("$url/ping/")
    }
}

def static Process startApp(name, version) {
    ProcessBuilder builder = new ProcessBuilder("build/exploded/${name}-${version}/bin/${name}")
    builder.inheritIO()
    builder.start()
}

