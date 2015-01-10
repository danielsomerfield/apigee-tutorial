import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClients
import org.gradle.api.GradleException


def static bintrayPublish(version) {
    def bintrayPublishURL = "https://api.bintray.com/content/danielsomerfield/maven/apigee-tutorial/${version}/publish"

    HttpPost post = new HttpPost(bintrayPublishURL)
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
            new AuthScope(null, -1),
            new UsernamePasswordCredentials(
                    System.getenv("BINTRAY_USERNAME"),
                    System.getenv("BINTRAY_PASSWORD")
            )
    );

    HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build().withCloseable { client ->
        client.execute(post).withCloseable() { response ->
            if (response.statusLine.statusCode != 200) {
                throw new GradleException("Bintray publish receive status code ${response.statusLine.statusCode}.")
            }
        }
    }
}

