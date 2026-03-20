package outboxpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WriteWithOutboxWorker implements Worker {
    @Override public String getTaskDefName() { return "ob_write_with_outbox"; }
    @Override public TaskResult execute(Task task) {
        String entityId = (String) task.getInputData().getOrDefault("entityId", "unknown");
        String outboxId = "OBX-" + System.currentTimeMillis();
        System.out.println("  [write] Entity " + entityId + " + outbox entry " + outboxId + " (atomic)");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("outboxId", outboxId);
        r.getOutputData().put("written", true);
        return r;
    }
}
