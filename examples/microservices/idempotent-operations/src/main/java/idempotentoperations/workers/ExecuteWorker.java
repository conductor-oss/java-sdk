package idempotentoperations.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExecuteWorker implements Worker {
    @Override public String getTaskDefName() { return "io_execute"; }
    @Override public TaskResult execute(Task task) {
        String action = (String) task.getInputData().getOrDefault("action", "unknown");
        String key = (String) task.getInputData().getOrDefault("idempotencyKey", "unknown");
        System.out.println("  [execute] " + action + " with key " + key);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("result", "success");
        return r;
    }
}
