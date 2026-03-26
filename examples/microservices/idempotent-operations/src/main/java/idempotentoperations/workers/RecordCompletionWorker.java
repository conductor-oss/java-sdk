package idempotentoperations.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecordCompletionWorker implements Worker {
    @Override public String getTaskDefName() { return "io_record_completion"; }
    @Override public TaskResult execute(Task task) {
        String key = (String) task.getInputData().getOrDefault("idempotencyKey", "unknown");
        System.out.println("  [record] Stored completion for " + key);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("recorded", true);
        return r;
    }
}
