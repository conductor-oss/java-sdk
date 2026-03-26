package slackintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Tracks delivery status of a posted Slack message.
 * Input: messageId, channel
 * Output: delivered, trackedAt
 *
 * This worker is always deterministic.— delivery tracking would use the Slack
 * Web API conversations.history to verify message delivery.
 */
public class TrackDeliveryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "slk_track_delivery";
    }

    @Override
    public TaskResult execute(Task task) {
        String messageId = (String) task.getInputData().get("messageId");
        if (messageId == null) {
            messageId = "unknown";
        }
        String channel = (String) task.getInputData().get("channel");
        if (channel == null) {
            channel = "general";
        }

        System.out.println("  [track] Message " + messageId + " delivered to #" + channel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delivered", true);
        result.getOutputData().put("trackedAt", java.time.Instant.now().toString());
        return result;
    }
}
