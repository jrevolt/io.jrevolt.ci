package io.jrevolt.ci.server;

import com.wordnik.swagger.jaxrs.config.BeanConfig;
import com.wordnik.swagger.jersey.listing.ApiListingResourceJSON;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
//import org.glassfish.jersey.server.spring.scope.RequestContextFilter;

public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
//		register(RequestContextFilter.class);
//		register(JacksonFeature.class);

        packages(getClass().getPackage().getName());
        packages("com.wordnik.swagger.jaxrs.json");

        register(com.wordnik.swagger.jersey.listing.ApiListingResourceJSON.class);

//        register(LoggingFilter.class);
//        register(ApiListingResourceJSON.class);
//        register(JerseyApiDeclarationProvider.class);
//        register(JerseyResourceListingProvider.class);

		register(LoggingFilter.class);
    }



}