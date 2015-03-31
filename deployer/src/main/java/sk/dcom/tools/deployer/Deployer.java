package sk.dcom.tools.deployer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.launcher.RepositorySupport;
import org.springframework.boot.launcher.mvn.Artifact;
import org.springframework.util.Assert;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 * @version $Id$
 */
public abstract class Deployer {

	static private Logger LOG = LoggerFactory.getLogger(Deployer.class);

	@Value("${deploy.home}")
	File deploymentHome;

	@Value("${as.address}")
	InetSocketAddress asAddress;

	@Value("${as.user}")
	String asUser;

	@Value("${as.password}")
	String asPassword;

	@Value("${as.deployment.context}")
	String asDeploymentContext;

	@Value("${nexus.repository}")
	URL repository;

	@Value("${nexus.username}")
	String repositoryUsername;

	@Value("${nexus.password}")
	String repositoryPassword;

	@Value("${artifact}")
	List<MvnUri> mvnuris;

	@Value("${restart}")
	boolean restart;

	@Value("${force}")
	boolean force;

	public void run() {
		Iterator<MvnUri> it = mvnuris.iterator();
		while (it.hasNext()) {
			MvnUri mvnuri = it.next();
			Artifact artifact = downloadArtifact(mvnuri);
			deploy(artifact, restart && !it.hasNext());
		}
	}

	Artifact downloadArtifact(MvnUri mvnuri) {
		Assert.notNull(mvnuri, "Undefined $artifact");
			Artifact artifact = RepositorySupport.resolve(mvnuri.asString());
		LOG.info("Resolved: {}, status: {}, file: {}", artifact.asString(), artifact.getStatus(), artifact.getFile());
		if (artifact.getError() != null) {
			LOG.error("Error resolving artifact", artifact.getError());
		}
		return artifact;
	}

	abstract void deploy(Artifact artifact, boolean restart);


}
