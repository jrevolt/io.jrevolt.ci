package sk.dcom.tools.deployer;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.launcher.mvn.Artifact;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 * @version $Id$
 */
@Component
@Profile("tomcat")
public class TomcatDeployer extends Deployer {

	@Value("${as.management.url}")
	URL managementUrl;

	@Override
	void deploy(Artifact artifact, boolean restart) {

		Assert.isTrue(!asDeploymentContext.isEmpty(), "Undefined $as.deployment.context");

		try {
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(
					new AuthScope(asAddress.getHostName(), asAddress.getPort()),
					new UsernamePasswordCredentials(asUser, asPassword));
			CloseableHttpClient client = HttpClients.custom()
					.setDefaultCredentialsProvider(credentialsProvider)
					.setDefaultRequestConfig(RequestConfig.custom().setExpectContinueEnabled(true).build())
					.build();
//			String tag = artifact.asString().replace(':', '#');
			String version = artifact.isSnapshot() ? artifact.getResolvedSnapshotVersion() : artifact.getVersion();
			URI uri = new URIBuilder(managementUrl.toURI())
					.setPath(managementUrl.getPath().replaceFirst("/+$", "/deploy"))
					.addParameter("path", asDeploymentContext)
//					.addParameter("tag", tag)
					.addParameter("version", version)
					.addParameter("update", "true")
					.build();
			HttpPut action = new HttpPut(uri);
			action.setEntity(new FileEntity(artifact.getFile()));
			CloseableHttpResponse response = client.execute(action);
			HttpEntity entity = response.getEntity();
			System.out.println(EntityUtils.toString(entity));
			response.close();
			client.close();
		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
