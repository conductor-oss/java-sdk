package finetuneddeployment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Deploys the validated model to a staging endpoint.
 */
public class DeployStagingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ftd_deploy_staging";
    }

    @Override
    public TaskResult execute(Task task) {
        String modelId = (String) task.getInputData().get("modelId");

        System.out.println("  [deploy] Deploying " + modelId + " to staging...");

        String endpoint = "https://staging.models.internal/" + modelId;
        System.out.println("  [deploy] Staging endpoint: " + endpoint);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("endpoint", endpoint);
        result.getOutputData().put("status", "running");
        result.getOutputData().put("gpuType", "A100");
        result.getOutputData().put("replicas", 2);
        return result;
    }
}
