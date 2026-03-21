package canaryrelease.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class DeployCanaryWorker implements Worker {
    @Override public String getTaskDefName() { return "cy_deploy_canary"; }

    @Override public TaskResult execute(Task task) {
        String appName = (String) task.getInputData().getOrDefault("appName", "unknown");
        String version = (String) task.getInputData().getOrDefault("version", "0.0.0");
        Object pct = task.getInputData().getOrDefault("trafficPercent", 5);
        System.out.println("  [deploy] Deploying " + appName + " v" + version + " at " + pct + "%...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("canaryInstances", 1);
        result.getOutputData().put("trafficPercent", pct);
        result.getOutputData().put("deployedAt", Instant.now().toString());
        return result;
    }
}
