package multiregiondeploy.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Deploys service to secondary regions.
 * Input: deploy_secondaryData
 * Output: deploy_secondary, processed, regions
 */
public class DeploySecondaryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mrd_deploy_secondary";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [secondary] Deployed to eu-west-1, ap-southeast-1");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deploy_secondary", true);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("regions", "eu-west-1, ap-southeast-1");
        return result;
    }
}
