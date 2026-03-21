package pubsubconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives a Pub/Sub message and extracts its data, encoding, and attributes.
 * The raw data is passed through as encodedData with base64 encoding marker.
 */
public class PsReceiveMessageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ps_receive_message";
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

        String publishTime = (String) task.getInputData().get("publishTime");
        if (publishTime == null || publishTime.isBlank()) {
            publishTime = "2026-01-15T10:00:00Z";
        }

        String data = (String) task.getInputData().get("data");
        if (data == null) {
            data = "";
        }

        Map<String, Object> attributes = (Map<String, Object>) task.getInputData().get("attributes");
        if (attributes == null) {
            attributes = Map.of();
        }

        System.out.println("  [ps_receive_message] Received message " + messageId + " from " + subscription);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("encodedData", data);
        result.getOutputData().put("encoding", "base64");
        result.getOutputData().put("attributes", attributes);
        result.getOutputData().put("receivedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
