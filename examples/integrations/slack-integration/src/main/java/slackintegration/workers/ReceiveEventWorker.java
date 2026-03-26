package slackintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Receives an incoming Slack event and extracts the event type and data.
 * Input: channel, eventType, payload
 * Output: eventType, data, receivedAt
 *
 * This worker is always deterministic.— event reception requires webhook
 * infrastructure (e.g., Slack Events API or Socket Mode).
 */
public class ReceiveEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "slk_receive_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "unknown";
        }
        String channel = (String) task.getInputData().get("channel");
        if (channel == null) {
            channel = "general";
        }
        Object payload = task.getInputData().get("payload");

        System.out.println("  [receive] Event: " + eventType + " on #" + channel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eventType", eventType);
        result.getOutputData().put("data", payload);
        result.getOutputData().put("receivedAt", java.time.Instant.now().toString());
        return result;
    }
}
