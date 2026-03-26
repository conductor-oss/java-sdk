package pubsubconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Acknowledges a Pub/Sub message after successful processing.
 * Records the subscription, message ID, and acknowledgement timestamp.
 */
public class PsAckMessageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ps_ack_message";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String subscription = (String) task.getInputData().get("subscription");
        if (subscription == null || subscription.isBlank()) {
            subscription = "projects/default/subscriptions/default-sub";
        }

        String messageId = (String) task.getInputData().get("messageId");
        if (messageId == null || messageId.isBlank()) {
            messageId = "unknown";
        }

        Map<String, Object> processingResult = (Map<String, Object>) task.getInputData().get("processingResult");
        if (processingResult == null) {
            processingResult = Map.of();
        }

        System.out.println("  [ps_ack_message] Acknowledging message " + messageId + " on " + subscription);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("acknowledged", true);
        result.getOutputData().put("ackedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
