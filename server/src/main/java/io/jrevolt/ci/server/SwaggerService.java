package io.jrevolt.ci.server;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
@Path("/")
public class SwaggerService {

    @GET
    @Path("{resource:.*}")
    @ApiOperation("resource")
    public Response resource(@PathParam("resource") String resource) throws IOException {
        URL url = getClass().getClassLoader().getResource(resource);
        URLConnection con = url != null ? url.openConnection() : null;
        String contentType = con != null ? con.getContentType() : null;
        if (url != null && url.getFile().endsWith(".css")) {
            contentType = "text/css";
        }
        if (url != null && url.getFile().endsWith(".js")) {
            contentType = "application/javascript";
        }
        if (url != null && url.getFile().endsWith(".min.map")) {
            contentType = "application/json";
        }
        return (url != null
                ? Response.ok(url.openStream(), contentType)
                : Response.status(Response.Status.NOT_FOUND))
                .build();
    }

}
