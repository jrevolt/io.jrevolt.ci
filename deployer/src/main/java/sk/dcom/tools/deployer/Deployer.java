package sk.dcom.tools.deployer;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
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

	/// database update ///

	@Value("${db.update}")
	boolean updateDatabase;


	@Value("${db.unlock}")
	boolean dbUnlock;

	@Value("${db.update.script}")
	String dbUpdateScript;

	@Value("${db.update.logLevel}")
	String dbUpdatelogLevel;

	@Value("${db.driver}")
	String jdbcDriver;

	@Value("${db.url}")
	String jdbcUrl;

	@Value("${db.user}")
	String jdbcUser;

	@Value("${db.password}")
	String jdbcPassword;

	@Value("${db.ignoreErrors}")
	boolean dbIgnoreErrors;

	@Value("${downloadOnly:false}")
	boolean downloadOnly;

	public void run() {
		Iterator<MvnUri> it = mvnuris.iterator();
		while (it.hasNext()) {
			MvnUri mvnuri = it.next();
			File archive = downloadArtifact(mvnuri);
			if (downloadOnly) {
				LOG.debug("Skipping deployment (--downloadOnly=true)");
				continue;
			}
			deploy(mvnuri, archive, restart && !it.hasNext());
		}
	}

	File downloadArtifact(MvnUri mvnuri) {

		Assert.notNull(mvnuri, "Undefined $artifact");

		try {
			URL url = new URL(repository, mvnuri.getPath());

			if (mvnuri.isSnapshot()) {
				URLConnection metadata = urlcon(new URL(url, "maven-metadata.xml"));

				try (InputStream in = metadata.getInputStream()) {

					// load the metadata and extract latest snapshot version
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
					Document doc = db.parse(in);
					XPathFactory xpf = XPathFactory.newInstance();
					XPath xpath = xpf.newXPath();
					String version = String.format(
							"%s-%s",
							xpath.evaluate("//versioning/snapshot/timestamp", doc),
							xpath.evaluate("//versioning/snapshot/buildNumber", doc)
					);

					mvnuri.resolvedSnapshotVersion = mvnuri.version.replaceFirst("-SNAPSHOT$", "-"+version);
				}
			}

			url = new URL(repository, mvnuri.getPath());
			URLConnection con = urlcon(url);

			File cache = new File(deploymentHome, "cache");
			File cached = new File(cache, mvnuri.getPath());
			cached.getParentFile().mkdirs();

			if (!cached.exists()) {
				try (InputStream in = con.getInputStream(); OutputStream out = new FileOutputStream(cached)) {
					LOG.debug("Downloading {}", url);
					IOUtils.copy(in, out);
				}
			}

			return cached;

		} catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	abstract void deploy(final MvnUri mvnuri, final File archive, boolean restart);

	URLConnection urlcon(URL url) {
		try {
			URLConnection con = url.openConnection();
			String auth = String.format("%s:%s", repositoryUsername, repositoryPassword);
			con.setRequestProperty("Authorization", String.format("Basic %s", DatatypeConverter.printBase64Binary(auth.getBytes("UTF-8"))));
			return con;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
