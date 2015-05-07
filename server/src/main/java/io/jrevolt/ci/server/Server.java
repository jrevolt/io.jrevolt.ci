package io.jrevolt.ci.server;

import com.messners.gitlab.api.GitLabApi;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jersey.config.JerseyJaxrsConfig;
import com.wordnik.swagger.models.Info;
import com.wordnik.swagger.models.Swagger;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@EnableScheduling
@EnableWebMvc
@ComponentScan
public class Server {

    @Bean
    public ServletRegistrationBean jersey() {
        return new ServletRegistrationBean(new ServletContainer(), "/rest/*") {
            {
                addInitParameter(ServletProperties.JAXRS_APPLICATION_CLASS, JerseyConfig.class.getName());
                addInitParameter("jersey.config.server.wadl.disableWadl", "true");
            }
            @Override
            public void onStartup(ServletContext ctx) throws ServletException {
                super.onStartup(ctx);
                Info info = new Info()
                        .title("io.jrevolt.ci")
                        .description("This is description");
                Swagger swagger = new Swagger().info(info);
                ctx.setAttribute("swagger", swagger);
                ctx.setAttribute("scanner", new DefaultJaxrsScanner());
            }
        };
    }

    @Bean
    public ServletRegistrationBean swagger() {
        return new ServletRegistrationBean(new JerseyJaxrsConfig()) {{
            addInitParameter("api.version", "1.0");
            addInitParameter("swagger.api.basepath", "/rest");
        }};
    }

//    @Bean
//    public FilterRegistrationBean filterEnableCORS() {
//        return new FilterRegistrationBean(new CorsFilter());
//    }

    @Bean
    GitLabApi gitlab(ServerCfg cfg) {
        return GitLabApi.create(
                cfg.getGitlab().getUrl().toExternalForm(),
                cfg.getGitlab().getUsername(),
                cfg.getGitlab().getPassword());
    }


}
