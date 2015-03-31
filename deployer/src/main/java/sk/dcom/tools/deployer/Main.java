package sk.dcom.tools.deployer;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ResourceBanner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.launcher.vault.VaultConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.System.getProperty;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 * @version $Id$
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Main {

	static Logger log() {
		return LoggerFactory.getLogger(Main.class);
	}

	static {
		// Nexus provides only brief/simplified/truncated metadata to Java user agent (URLConnection)
		System.setProperty("http.agent", "JRevolt Deployer");

		// Spring Boot: Support $HOME/.deployment/application[-*].properties
		System.setProperty("deploy.home", String.format("%s/%s", getProperty("user.home"), ".deploy"));
		System.setProperty("spring.config.location", "file:///${deploy.home}/");
	}

	@Autowired
	ApplicationContext ctx;

	@Value("${all}")
	String value;

	void run() {
		ctx.getBean(Deployer.class).run();
	}

	private void help() {
		try {
			InputStream in = getClass().getResource("help.txt").openStream();
			IOUtils.copy(in, System.out);
		} catch (IOException e) {
			log().error(e.toString());
		}
	}

	static public void main(String[] args) {
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder()
				.banner(new ResourceBanner(new UrlResource(Main.class.getResource("banner.txt"))))
				.sources(Main.class, VaultConfiguration.class)
				.environment(VaultConfiguration.initStandardEnvironment())
				.run(args);
		//String property = ctx.getEnvironment().getProperty("encrypted.value", String.class);
		Main main = ctx.getBean(Main.class);
		main.run();
		ctx.close();
	}

}
