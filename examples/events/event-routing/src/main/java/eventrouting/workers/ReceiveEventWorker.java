package eventrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives an incoming event and passes through its domain and data,
 * stamping a receivedAt timestamp.
 */
public class ReceiveEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eo_receive_event";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        String eventDomain = (String) task.getInputData().get("eventDomain");
        Object eventData = task.getInputData().get("eventData");

        if (eventId == null) eventId = "unknown";
        if (eventDomain == null) eventDomain = "unknown";

        System.out.println("  [eo_receive_event] Received event: " + eventId
                + " domain=" + eventDomain);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eventDomain", eventDomain);
        result.getOutputData().put("eventData", eventData);
        result.getOutputData().put("receivedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
