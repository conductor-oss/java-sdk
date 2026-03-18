package deadletterevents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives an incoming event, normalizes retryCount to an integer,
 * and stamps a receivedAt timestamp.
 */
public class DlReceiveEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dl_receive_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        String eventType = (String) task.getInputData().get("eventType");
        Object payload = task.getInputData().get("payload");
        Object retryCountRaw = task.getInputData().get("retryCount");

        int retryCount = 0;
        if (retryCountRaw != null) {
            try {
                retryCount = Integer.parseInt(String.valueOf(retryCountRaw));
            } catch (NumberFormatException e) {
                retryCount = 0;
            }
        }

        System.out.println("  [dl_receive_event] Received event: " + eventId + " type=" + eventType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eventId", eventId);
        result.getOutputData().put("eventType", eventType);
        result.getOutputData().put("payload", payload);
        result.getOutputData().put("retryCount", retryCount);
        result.getOutputData().put("receivedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
