package featureflags.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class LogUsageWorker implements Worker {
    @Override public String getTaskDefName() { return "ff_log_usage"; }

    @Override public TaskResult execute(Task task) {
        String feature = (String) task.getInputData().getOrDefault("feature", "unknown");
        String flagStatus = (String) task.getInputData().getOrDefault("flagStatus", "unknown");
        System.out.println("  [log] Logged flag usage: " + feature + " = " + flagStatus + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        return result;
    }
}
