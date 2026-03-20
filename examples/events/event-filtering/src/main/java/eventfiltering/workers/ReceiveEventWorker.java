package eventfiltering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives an incoming event and enriches it with metadata.
 * Input:  eventId, eventType, severity, payload
 * Output: eventType, severity, payload, metadata (source, receivedAt)
 */
public class ReceiveEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ef_receive_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        String eventType = (String) task.getInputData().get("eventType");
        String severity = (String) task.getInputData().get("severity");
        Object payload = task.getInputData().get("payload");

        if (eventId == null) eventId = "";
        if (eventType == null) eventType = "";
        if (severity == null) severity = "";

        System.out.println("  [ef_receive_event] Event: " + eventId
                + ", type: " + eventType + ", severity: " + severity);

        Map<String, String> metadata = Map.of(
                "source", "monitoring-agent",
                "receivedAt", "2026-01-15T10:00:00Z"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eventType", eventType);
        result.getOutputData().put("severity", severity);
        result.getOutputData().put("payload", payload);
        result.getOutputData().put("metadata", metadata);
        return result;
    }
}
