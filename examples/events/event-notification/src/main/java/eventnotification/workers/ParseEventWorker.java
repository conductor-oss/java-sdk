package eventnotification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Parses an incoming event and prepares notification content for all channels.
 * Input: event (map with type, message), recipientId
 * Output: recipientId, subject, body, smsMessage
 */
public class ParseEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "en_parse_event";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> event = (Map<String, Object>) task.getInputData().get("event");
        if (event == null) {
            event = Map.of();
        }

        String recipientId = (String) task.getInputData().get("recipientId");
        if (recipientId == null) {
            recipientId = "unknown";
        }

        String type = (String) event.get("type");
        if (type == null) {
            type = "general";
        }

        String message = (String) event.get("message");
        String body = (message != null) ? message : "You have a new notification";
        String smsMessage = "[" + type + "] " + body;

        System.out.println("  [en_parse_event] Event type: " + type + ", recipient: " + recipientId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recipientId", recipientId);
        result.getOutputData().put("subject", "Notification: " + type);
        result.getOutputData().put("body", body);
        result.getOutputData().put("smsMessage", smsMessage);
        return result;
    }
}
