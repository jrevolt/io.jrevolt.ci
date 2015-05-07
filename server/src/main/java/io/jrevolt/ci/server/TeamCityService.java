package io.jrevolt.ci.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
@Component
public class TeamCityService {

    @Autowired
    ServerCfg cfg;

    public void runBuild(String projectName, String branchName) {
        try {
            URI uri = UriBuilder.fromUri(cfg.getTeamcity().getUrl().toExternalForm())
                    .path("httpAuth/action.html")
                    .queryParam("add2Queue", projectName)
                    .queryParam("name", "teamcity.build.branch")
                    .queryParam("value", branchName)
                    .build();
            HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
            String auth = String.format("%s:%s", cfg.getTeamcity().getUsername(), cfg.getTeamcity().getPassword());
            con.setRequestProperty(
                    "Authorization",
                    String.format("Basic %s", DatatypeConverter.printBase64Binary(auth.getBytes("UTF-8"))));
            int responseCode = con.getResponseCode();
            String responseMessage = con.getResponseMessage();
            Log.debug(this, "Building branch {} of project {} ({} : {}).  URL: {}",
                    branchName, projectName, responseCode, responseMessage, uri);
        } catch (IOException e) {
            throw new UnsupportedOperationException(e);
        }
    }

}
