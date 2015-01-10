import groovy.transform.Field
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.gradle.api.GradleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Field private static Logger logger = LoggerFactory.getLogger("BuildUtils.groovy")

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

def static waitForPing(long timeInMillis) {
    def url = System.getenv("HELLO_SERVICE_ROOT") ?: "http://localhost:8080"
    logger.info("waiting for $timeInMillis milliseconds until ping to $url returns 200")
    waitUntil(timeInMillis) {
        ping("$url/ping/")
    }
}

def static Process startApp(name, version) {
    ProcessBuilder builder = new ProcessBuilder("build/exploded/${name}-${version}/bin/${name}")
    builder.inheritIO()
    builder.start()
}