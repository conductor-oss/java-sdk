package multitenancy.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class LogUsageWorker implements Worker {
    @Override public String getTaskDefName() { return "mt_log_usage"; }
    @Override public TaskResult execute(Task task) {
        String tenantId = (String) task.getInputData().getOrDefault("tenantId", "unknown");
        Object cost = task.getInputData().getOrDefault("cost", 0);
        System.out.println("  [usage] Logged: " + tenantId + " - $" + cost);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("logged", true);
        return r;
    }
}
