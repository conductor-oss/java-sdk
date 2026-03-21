package multiregiondeploy.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Deploys service to the primary region.
 * Input: service, version, regions
 * Output: deploy_primaryId, success, region
 */
public class DeployPrimaryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mrd_deploy_primary";
    }

    @Override
    public TaskResult execute(Task task) {
        String service = (String) task.getInputData().get("service");
        String version = (String) task.getInputData().get("version");

        if (service == null) service = "unknown-service";
        if (version == null) version = "0.0.0";

        System.out.println("  [primary] Deployed " + service + ":" + version + " to us-east-1");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deploy_primaryId", "DEPLOY_PRIMARY-1362");
        result.getOutputData().put("success", true);
        result.getOutputData().put("service", service);
        result.getOutputData().put("version", version);
        result.getOutputData().put("region", "us-east-1");
        return result;
    }
}
