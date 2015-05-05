package io.jrevolt.ci.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.launcher.vault.VaultConfiguration;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class Main {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(Server.class, VaultConfiguration.class)
                .environment(VaultConfiguration.initStandardEnvironment())
                .run(args);
    }

}
