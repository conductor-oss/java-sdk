package eventsourcing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.Map;

public class ValidateEventWorker implements Worker {
    @Override public String getTaskDefName() { return "es_validate_event"; }
    @Override public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().getOrDefault("eventType", "UNKNOWN");
        String aggregateId = (String) task.getInputData().getOrDefault("aggregateId", "unknown");
        System.out.println("  [validate] " + eventType + " for " + aggregateId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("validatedEvent", Map.of("type", eventType, "data", task.getInputData().getOrDefault("eventData", Map.of()), "timestamp", Instant.now().toString()));
        return r;
    }
}
