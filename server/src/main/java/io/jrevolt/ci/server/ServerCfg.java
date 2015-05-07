package io.jrevolt.ci.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
@Component
@ConfigurationProperties(prefix = "ci")
public class ServerCfg {

    private GitLab gitlab;
    private TeamCity teamcity;
    private File bash;
    private File workspace;

    public GitLab getGitlab() {
        return gitlab;
    }

    public void setGitlab(GitLab gitlab) {
        this.gitlab = gitlab;
    }

    public TeamCity getTeamcity() {
        return teamcity;
    }

    public void setTeamcity(TeamCity teamcity) {
        this.teamcity = teamcity;
    }

    public File getBash() {
        return bash;
    }

    public void setBash(File bash) {
        this.bash = bash;
    }

    public File getWorkspace() {
        return workspace;
    }

    public void setWorkspace(File workspace) {
        this.workspace = workspace;
    }

    static public abstract class EndpointCfg {
        private URL url;
        private String username;
        private String password;

        public URL getUrl() {
            return url;
        }

        public void setUrl(URL url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    static public class GitLab extends EndpointCfg {}

    static public class TeamCity extends EndpointCfg {}

}
