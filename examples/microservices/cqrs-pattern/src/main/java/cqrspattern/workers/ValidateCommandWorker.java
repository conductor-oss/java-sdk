package cqrspattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class ValidateCommandWorker implements Worker {
    @Override public String getTaskDefName() { return "cqrs_validate_command"; }
    @Override public TaskResult execute(Task task) {
        String command = (String) task.getInputData().getOrDefault("command", "UNKNOWN");
        String aggregateId = (String) task.getInputData().getOrDefault("aggregateId", "unknown");
        System.out.println("  [validate] Command: " + command + " for " + aggregateId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("valid", true);
        r.getOutputData().put("event", Map.of("type", "ITEM_UPDATED", "data", task.getInputData().getOrDefault("data", Map.of())));
        return r;
    }
}
