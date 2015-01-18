import groovy.json.JsonSlurper
import groovy.transform.Field
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.FileEntity
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClients
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Field private static Logger logger = LoggerFactory.getLogger("ApigeeUtils.groovy")

def static deployToApigee(String version) {
    def proxyBundleName = "api-proxy-bundle-${version}.zip"
    HttpPost post = new HttpPost("https://api.enterprise.apigee.com/v1/o/danielsomerfield/apis?action=import&name=apigee-tutorial")
    post.addHeader("Content-Type", "application/octet-stream")
    post.setEntity(new FileEntity(new File("api-proxy-bundle/build/distributions/$proxyBundleName"), "binary/octet-stream"))
    def response = doHttpRequest(post)
    logger.info("Uploaded revision ${response.revision}")

    HttpPost deployPost = new HttpPost("https://api.enterprise.apigee.com/v1/o/danielsomerfield/environments/prod/apis/apigee-tutorial/revisions/${response.revision}/deployments?override=true")
    deployPost.addHeader("Content-type", "application/x-www-form-urlencoded")
    doHttpRequest(deployPost)
}

def static doHttpRequest(HttpUriRequest request) {
    String username = validate("APIGEE_USERNAME")
    String password = validate("APIGEE_PASSWORD")

    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
            new AuthScope(null, -1),
            new UsernamePasswordCredentials(username, password));

    HttpClients.custom()
            .setDefaultCredentialsProvider(credsProvider)
            .build().withCloseable() { client ->
        CloseableHttpResponse response = client.execute(request)
        if (response.statusLine.statusCode < 200 || response.statusLine.statusCode >= 300) {
            throw new GradleException("Deploy failed with response code ${response.statusLine.statusCode}")
        }

        response.getEntity().content.withCloseable { i ->
            return new JsonSlurper().parse(new InputStreamReader(i))
        }
    }
}

def static validate(String varName) {
    String value = System.getenv(varName);
    if (!value) throw new InvalidUserDataException("Missing required environment variable '$varName'")
    value
}