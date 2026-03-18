package bluegreendeploy.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyDeploymentWorker implements Worker {
    @Override public String getTaskDefName() { return "bg_verify_deployment"; }

    @Override public TaskResult execute(Task task) {
        String appName = (String) task.getInputData().getOrDefault("appName", "unknown");
        String version = (String) task.getInputData().getOrDefault("version", "0.0.0");
        String activeEnv = (String) task.getInputData().getOrDefault("activeEnv", "green");
        System.out.println("  [verify] Verifying " + appName + " v" + version + " on " + activeEnv + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "success");
        result.getOutputData().put("errorRate", "0.01%");
        result.getOutputData().put("p99Latency", "52ms");
        result.getOutputData().put("rollbackAvailable", true);
        return result;
    }
}
