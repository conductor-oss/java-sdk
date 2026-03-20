package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class EmitEventWorker implements Worker {
    @Override public String getTaskDefName() { return "edm_emit_event"; }
    @Override public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().getOrDefault("eventType", "UNKNOWN");
        String source = (String) task.getInputData().getOrDefault("source", "unknown");
        String eventId = "EVT-" + System.currentTimeMillis();
        System.out.println("  [emit] " + eventType + " from " + source + " -> " + eventId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("eventId", eventId);
        r.getOutputData().put("timestamp", Instant.now().toString());
        return r;
    }
}
