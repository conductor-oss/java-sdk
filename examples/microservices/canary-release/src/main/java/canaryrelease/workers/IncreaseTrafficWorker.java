package canaryrelease.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class IncreaseTrafficWorker implements Worker {
    @Override public String getTaskDefName() { return "cy_increase_traffic"; }

    @Override public TaskResult execute(Task task) {
        Object pct = task.getInputData().getOrDefault("trafficPercent", 50);
        System.out.println("  [increase] Increasing traffic to " + pct + "%...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("trafficPercent", pct);
        result.getOutputData().put("canaryInstances", 5);
        result.getOutputData().put("updatedAt", Instant.now().toString());
        return result;
    }
}
