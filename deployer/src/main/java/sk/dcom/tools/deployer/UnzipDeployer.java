package sk.dcom.tools.deployer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
@Component
@Profile("unzip")
public class UnzipDeployer extends Deployer {

	static private Logger LOG = LoggerFactory.getLogger(UnzipDeployer.class);

	@Value("${deploy.dir}")
	File deployDir;

	@Value("${unzip.strip.components:0}")
	int stripComponents;


	@Override
	void deploy(MvnUri mvnuri, File archive, boolean restart) {
		try {
			ZipFile zip = new ZipFile(archive);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry ze = entries.nextElement();
				String name = ze.getName().replaceFirst("^/+", "");
				int strip = stripComponents;
				while (strip > 0) {
					name = name.replaceFirst("^[^/]*", "");
					strip--;
				}
				File dst = new File(deployDir, name);
				if (ze.isDirectory()) {
					LOG.debug("Writing {}", dst);
					dst.mkdirs();
				} else {
					dst.getParentFile().mkdirs();
					Files.copy(zip.getInputStream(ze), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException e) {
			throw new UnsupportedOperationException(e);
		}
	}
}
