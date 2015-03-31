package sk.dcom.tools.deployer;

import org.apache.commons.lang3.StringUtils;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.controller.client.helpers.standalone.ServerUpdateActionResult;
import org.jboss.dmr.ModelNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.launcher.mvn.Artifact;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 * @version $Id$
 */
@Component
@Profile("jboss")
public class JBossDeployer extends Deployer {

	static private Logger LOG = LoggerFactory.getLogger(JBossDeployer.class);

	void deploy(Artifact artifact, boolean restart) {

		ModelControllerClient client = DomainClient.Factory.create(asAddress.getAddress(), asAddress.getPort());
		ServerDeploymentManager manager = ServerDeploymentManager.Factory.create(client);

		String name = artifact.getArtifactId();
		String runtimeName = artifact.getFile().getName();
		String currentRuntimeName = getCurrentRuntimeName(artifact, client);

		DeploymentPlan plan;
		ServerDeploymentPlanResult result;

		try {

			if (StringUtils.equals(currentRuntimeName, runtimeName) && !force) {
				LOG.info("Requested version is already deployed. Nothing to do.");
				return;
			}

			LOG.info("Preparing deployment of {}, name={}, runtimeName={}", artifact.asString(), name, runtimeName);


			LOG.info("Undeploying {}", currentRuntimeName);

			if (currentRuntimeName != null) {
				plan = manager.newDeploymentPlan().undeploy(name).build();
				result = executePlan(manager, plan);
				report(plan, result);
			}

			LOG.info("Deploying {}", runtimeName);

			plan = currentRuntimeName != null
					? manager.newDeploymentPlan()
							.replace(name, runtimeName, new FileInputStream(artifact.getFile()))
							.deploy(name)
							.build()
					: manager.newDeploymentPlan()
							.add(name, runtimeName, new FileInputStream(artifact.getFile()))
							.deploy(name)
							.build();
			result = executePlan(manager, plan);
			report(plan, result);

			currentRuntimeName = getCurrentRuntimeName(artifact, client);

			if (StringUtils.equals(runtimeName, currentRuntimeName)) {
				LOG.info("SUCCESS");
			} else {
				throw new DeploymentException("Failed to deploy "+runtimeName, null);
			}

			if (restart) {
				restartServer(client);
			}

		} catch (DeploymentException e) {
			if (currentRuntimeName != null) {
				LOG.error("Restoring {}. Reason: {}", currentRuntimeName, e);
				redeploy(manager, name);
			}
			throw e;

		} catch (IOException e) {
			throw new RuntimeException(e);

		} finally {
			try {
				manager.close();
			} catch (IOException ignore) {
			}
			try {
				client.close();
			} catch (IOException ignore) {
			}
		}
	}

	private void redeploy(ServerDeploymentManager manager, String name) {
		DeploymentPlan plan = manager.newDeploymentPlan().deploy(name).build();
		ServerDeploymentPlanResult  result = executePlan(manager, plan);
		report(plan, result);
	}

	private ServerDeploymentPlanResult executePlan(ServerDeploymentManager manager, DeploymentPlan plan) {
		try {
			return manager.execute(plan).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new DeploymentException(e);
		}
	}

	private String getCurrentRuntimeName(Artifact artifact, ModelControllerClient client) {
		try {
			ModelNode op = new ModelNode();
			op.get("address").add("deployment", artifact.getArtifactId());
			op.get("operation").set("read-attribute");
			op.get("name").set("runtime-name");

			ModelNode result = client.execute(op);

			String currentRuntimeName = "success".equals(result.get("outcome").asString()) ? result.get("result").asString() : null;

			LOG.info("Currently deployed: {}", StringUtils.defaultIfEmpty(currentRuntimeName, "<none>"));
			return currentRuntimeName;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void restartServer(ModelControllerClient client) {
		try {
			LOG.info("Restarting server...");
			ModelNode op = new ModelNode();
			op.get("operation").set("shutdown");
			op.get("restart").set("true");
			ModelNode result = client.execute(op);
			LOG.info("Result: {}", result);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isSuccess(DeploymentPlan plan, ServerDeploymentPlanResult result) {
		List<DeploymentAction> actions = plan.getDeploymentActions();
		boolean success = true;
		for (DeploymentAction action : actions) {
			ServerUpdateActionResult.Result r = result.getDeploymentActionResult(action.getId()).getResult();
			switch (r) {
				case EXECUTED:
				case CONFIGURATION_MODIFIED_REQUIRES_RESTART:
					success &= true; break;
				case ROLLED_BACK:
				case NOT_EXECUTED:
					return false;
				default:
					throw new AssertionError(r);
			}
		}
		return success;
	}

	private void report(DeploymentPlan plan, ServerDeploymentPlanResult result) {
		for (DeploymentAction action : plan.getDeploymentActions()) {
			Throwable t = result.getDeploymentActionResult(action.getId()).getDeploymentException();
			LOG.info("{} :: {} {} {}",
						result.getDeploymentActionResult(action.getId()).getResult(),
						action.getType(),
						action.getDeploymentUnitUniqueName(),
						t != null ? t : "");
		}
	}
}
