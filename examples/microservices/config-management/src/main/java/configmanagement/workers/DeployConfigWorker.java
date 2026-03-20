package configmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class DeployConfigWorker implements Worker {
    @Override public String getTaskDefName() { return "cf_deploy_config"; }

    @Override public TaskResult execute(Task task) {
        String env = (String) task.getInputData().getOrDefault("environment", "unknown");
        System.out.println("  [deploy] Deploying config to " + env + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deploymentId", "cfg-deploy-20240301-001");
        result.getOutputData().put("nodesUpdated", 8);
        result.getOutputData().put("deployedAt", Instant.now().toString());
        return result;
    }
}
