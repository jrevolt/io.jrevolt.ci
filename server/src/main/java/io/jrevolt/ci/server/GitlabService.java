package io.jrevolt.ci.server;

import com.messners.gitlab.api.GitLabApi;
import com.messners.gitlab.api.GitLabApiException;
import com.messners.gitlab.api.models.Branch;
import com.messners.gitlab.api.models.Project;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
@Path("gitlab")
@Api("gitlab")
@Component
public class GitlabService {

    @Autowired
    ServerCfg cfg;

    @Autowired
    GitLabApi gitlab;

    @Autowired
    TeamCityService teamcity;

    ExecutorService executor = new ForkJoinPool(10);

    Map<String, Object> LOCKS = new HashMap<>();

    /// system hooks ///

    void onSystemEvent() {}

    void onProjectCreated() {}
    void onProjectDeleted() {}

    void onUserCreated() {}
    void onUserDeleted() {}

    void onTeamMemberCreated() {}
    void onTeamMemberDeleted() {}

    /// project web hooks ///

    void onPush() {}
    void onTag() {}
    void onIssue() {}

    @Path("webhook")
    @POST
    @ApiOperation("webhook")
    public Response webhook(String event) {
        Log.debug(this, event);
        JSONObject json = new JSONObject(event);
        String kind = json.optString("object_kind");
        if (kind.equals("merge_request")) {
            executor.submit(() -> onMergeRequest(json));
        }
        return Response.accepted().build();
    }

    private void onMergeRequest(JSONObject json) {
        try {
            JSONObject attrs = json.getJSONObject("object_attributes");
            Integer id = attrs.getInt("id");
            Integer iid = attrs.getInt("iid");
            String state = attrs.getString("state");
            Integer sourceProjectId = attrs.getInt("source_project_id");
            Integer targetProjectId = attrs.getInt("target_project_id");
            String sourceBranch = attrs.getString("source_branch");
            String targetBranch = attrs.getString("target_branch");
            Project sourceProject = gitlab.getProjectApi().getProject(sourceProjectId);
            Project targetProject = gitlab.getProjectApi().getProject(targetProjectId);
            if (state.matches("(opened|reopened)")) {
                createMergeRequestBranch(targetProject, iid, sourceProject.getSshUrlToRepo(), sourceBranch, targetBranch);
            } else if (state.matches("(closed|merged)")) {
                cleanupMergeRequest(targetProject, iid);
            }
        } catch (GitLabApiException e) {
            throw new UnsupportedOperationException(e);
        }
    }


    synchronized Object getLock(Project project) {
        Object o = LOCKS.get(project.getPathWithNamespace());
        if (o == null) {
            o = new Object();
            LOCKS.put(project.getPathWithNamespace(), o);
        }
        return o;
    }

    void createMergeRequestBranch(Project targetProject, Integer requestId, String srcrepo, String srcbranch, String dstbranch) {
        synchronized (getLock(targetProject)) {
            String mergeRequestName = getMergeRequestName(requestId);
            String mergeRequestBranch = getMergeRequestBranchName(requestId);

            try {
                boolean done = gitlab.getRepositoryApi().getBranches(targetProject.getId()).stream()
                        .map(Branch::getName).collect(Collectors.toSet())
                        .contains(mergeRequestBranch);
                if (done) {
                    Log.debug(this, "Skip processing: merge request branch already exists: {}", mergeRequestBranch);
                    return;
                }
            } catch (GitLabApiException e) {
                throw new UnsupportedOperationException(e);
            }

            File dir = new File(cfg.getWorkspace(), targetProject.getPathWithNamespace());
            init(targetProject, dir);
            File script = deployScript(dir);
            CommandLine cmdline = new CommandLine(cfg.getBash())
                    .addArgument(script.getName())
                    .addArgument("onMergeRequestCreated")
                    .addArgument(requestId.toString())
                    .addArgument(srcrepo)
                    .addArgument(srcbranch)
                    .addArgument(dstbranch)
                    .addArgument(mergeRequestName)
                    .addArgument(mergeRequestBranch);
            execute(dir, cmdline);
        }
        teamcity.runBuild(getTeamCityProjectName(targetProject), getMergeRequestBranchName(requestId));
    }

    void cleanupMergeRequest(Project targetProject, Integer requestId) {
        synchronized (getLock(targetProject)) {

            String mergeRequestName = getMergeRequestName(requestId);
            String mergeRequestBranch = getMergeRequestBranchName(requestId);

            try {
                boolean done =! gitlab.getRepositoryApi()
                        .getBranches(targetProject.getId()).stream().map(Branch::getName)
                        .collect(Collectors.toSet()).contains(mergeRequestBranch);
                if (done) {
                    Log.debug(this, "Skip processing: merge request branch does not exist: {}", mergeRequestBranch);
                    return;
                }
            } catch (GitLabApiException e) {
                throw new UnsupportedOperationException(e);
            }

            File dir = new File(cfg.getWorkspace(), targetProject.getPathWithNamespace());
            init(targetProject, dir);
            File script = deployScript(dir);
            CommandLine cmdline = new CommandLine(cfg.getBash())
                    .addArgument(script.getName())
                    .addArgument("onMergeRequestClosed")
                    .addArgument(requestId.toString())
                    .addArgument(mergeRequestName)
                    .addArgument(mergeRequestBranch);
            execute(dir, cmdline);
        }

    }

    void init(Project project, File dir) {
        if (new File(dir, ".initialized").exists()) { return; }
        File script = deployScript(dir);
        CommandLine cmdline = new CommandLine(cfg.getBash())
                .addArgument(script.getName())
                .addArgument("init")
                .addArgument(project.getSshUrlToRepo());
        execute(dir, cmdline);
    }

    private Executor getExecutor(File dir) {
        Executor exec = new DefaultExecutor();
        exec.setWorkingDirectory(dir);
        exec.setStreamHandler(new PumpStreamHandler(System.out));
        return exec;
    }

    void execute(File dir, CommandLine cmdline) {
        try {
            UUID uuid = UUID.randomUUID();
            Log.debug(this, "Executing {} : {}", uuid, cmdline);
            getExecutor(dir).execute(cmdline);
            Log.debug(this, "Execution {} completed", uuid);
        } catch (IOException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private File deployScript(File dir) {
        try {
            File script = new File(dir, "repo.sh");
            URL url = getClass().getResource(script.getName());
            FileUtils.copyURLToFile(url, script);
            return script;
        } catch (IOException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    String getTeamCityProjectName(Project project) {
        return project.getDescription().replaceFirst("(?m)^teamcity: *(.*)$", "$1");
    }

    String getMergeRequestName(int iid) {
        return "MR-"+iid;
    }

    String getMergeRequestBranchName(int iid) {
        return "merge/"+getMergeRequestName(iid);
    }

}
