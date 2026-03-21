package webhookretry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Prepares webhook delivery by validating and packaging the URL and payload.
 * Input: webhookUrl, payload
 * Output: url, payload, preparedAt
 */
public class PrepareWebhookWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wr_prepare_webhook";
    }

    @Override
    public TaskResult execute(Task task) {
        String webhookUrl = (String) task.getInputData().get("webhookUrl");
        if (webhookUrl == null) {
            webhookUrl = "https://default.example.com/webhook";
        }

        Object payload = task.getInputData().get("payload");
        if (payload == null) {
            payload = Map.of();
        }

        System.out.println("  [wr_prepare_webhook] Preparing webhook for: " + webhookUrl);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("url", webhookUrl);
        result.getOutputData().put("payload", payload);
        result.getOutputData().put("preparedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
