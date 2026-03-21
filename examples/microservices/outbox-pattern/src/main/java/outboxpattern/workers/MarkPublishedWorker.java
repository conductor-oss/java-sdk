package outboxpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MarkPublishedWorker implements Worker {
    @Override public String getTaskDefName() { return "ob_mark_published"; }
    @Override public TaskResult execute(Task task) {
        String outboxId = (String) task.getInputData().getOrDefault("outboxId", "unknown");
        System.out.println("  [mark] Outbox entry " + outboxId + " marked as published");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("marked", true);
        return r;
    }
}
