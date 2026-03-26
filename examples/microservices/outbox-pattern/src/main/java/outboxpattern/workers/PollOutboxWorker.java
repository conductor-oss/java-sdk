package outboxpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class PollOutboxWorker implements Worker {
    @Override public String getTaskDefName() { return "ob_poll_outbox"; }
    @Override public TaskResult execute(Task task) {
        String outboxId = (String) task.getInputData().getOrDefault("outboxId", "unknown");
        System.out.println("  [poll] Found unpublished event: " + outboxId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("event", Map.of("type", "ORDER_CREATED", "entityId", "ORD-1"));
        r.getOutputData().put("destination", "orders-topic");
        return r;
    }
}
