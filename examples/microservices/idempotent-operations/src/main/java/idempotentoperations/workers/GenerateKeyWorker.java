package idempotentoperations.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateKeyWorker implements Worker {
    @Override public String getTaskDefName() { return "io_generate_key"; }
    @Override public TaskResult execute(Task task) {
        String opId = (String) task.getInputData().getOrDefault("operationId", "unknown");
        String action = (String) task.getInputData().getOrDefault("action", "unknown");
        String key = opId + "-" + action;
        System.out.println("  [key] Generated idempotency key: " + key);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("key", key);
        return r;
    }
}
