package canaryrelease.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class FullRolloutWorker implements Worker {
    @Override public String getTaskDefName() { return "cy_full_rollout"; }

    @Override public TaskResult execute(Task task) {
        String appName = (String) task.getInputData().getOrDefault("appName", "unknown");
        String version = (String) task.getInputData().getOrDefault("version", "0.0.0");
        System.out.println("  [rollout] Full rollout of " + appName + " v" + version + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "success");
        result.getOutputData().put("trafficPercent", 100);
        result.getOutputData().put("complete", true);
        result.getOutputData().put("completedAt", Instant.now().toString());
        return result;
    }
}
