package bluegreendeploy.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class PrepareGreenWorker implements Worker {
    @Override public String getTaskDefName() { return "bg_prepare_green"; }

    @Override public TaskResult execute(Task task) {
        String appName = (String) task.getInputData().getOrDefault("appName", "unknown");
        String newVersion = (String) task.getInputData().getOrDefault("newVersion", "0.0.0");
        System.out.println("  [prepare] Deploying " + appName + " v" + newVersion + " to green...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("environment", "green");
        result.getOutputData().put("containersDeployed", 4);
        result.getOutputData().put("imageTag", appName + ":" + newVersion);
        result.getOutputData().put("readyAt", Instant.now().toString());
        return result;
    }
}
