package eventfanout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends a notification about the event.
 * Input: eventId, eventType
 * Output: result:"notified", channel:"slack"
 */
public class NotificationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fo_notification";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "unknown";
        }

        System.out.println("  [fo_notification] Notifying via slack for event: " + eventId + " type: " + eventType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "notified");
        result.getOutputData().put("channel", "slack");
        return result;
    }
}
