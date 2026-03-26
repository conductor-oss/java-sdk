package eventdrivenworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives an incoming event and enriches it with metadata.
 * Input: eventId, eventType, eventData
 * Output: eventType, eventData, metadata (source, receivedAt)
 */
public class ReceiveEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ed_receive_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "unknown";
        }

        Object eventData = task.getInputData().get("eventData");
        if (eventData == null) {
            eventData = Map.of();
        }

        System.out.println("  [ed_receive_event] Received event type: " + eventType);

        Map<String, String> metadata = Map.of(
                "source", "event-bus",
                "receivedAt", "2026-01-15T10:00:00Z"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eventType", eventType);
        result.getOutputData().put("eventData", eventData);
        result.getOutputData().put("metadata", metadata);
        return result;
    }
}
