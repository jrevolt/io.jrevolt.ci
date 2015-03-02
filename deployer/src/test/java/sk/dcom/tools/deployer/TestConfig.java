package sk.dcom.tools.deployer;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.nio.file.Files;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 * @version $Id$
 */
@Configuration
@ComponentScan(basePackageClasses = TestConfig.class)
@PropertySource(value="classpath:application.properties")
public class TestConfig {
	static {
		try {
			System.setProperty("secure.file", Files.createTempFile("TestConfig", "properties").toString());
			System.setProperty("secure.key", Files.createTempFile("TestConfig", "key").toString());
		} catch (IOException e) {
			throw new UnsupportedOperationException(e);
		}
	}
}
