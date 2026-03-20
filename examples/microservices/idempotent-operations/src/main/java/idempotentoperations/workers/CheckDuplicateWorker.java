package idempotentoperations.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckDuplicateWorker implements Worker {
    @Override public String getTaskDefName() { return "io_check_duplicate"; }
    @Override public TaskResult execute(Task task) {
        String key = (String) task.getInputData().getOrDefault("idempotencyKey", "unknown");
        System.out.println("  [check] Key " + key + ": not seen before");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("isDuplicate", false);
        return r;
    }
}
