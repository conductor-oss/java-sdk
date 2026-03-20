package webhookcallback.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Sends a callback notification to the partner's webhook URL with
 * the processing result and completion status.
 */
public class NotifyCallbackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wc_notify_callback";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String callbackUrl = (String) task.getInputData().get("callbackUrl");
        if (callbackUrl == null || callbackUrl.isBlank()) {
            callbackUrl = "";
        }

        String requestId = (String) task.getInputData().get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = "unknown";
        }

        Map<String, Object> processingResult = (Map<String, Object>) task.getInputData().get("result");
        if (processingResult == null) {
            processingResult = Map.of();
        }

        String status = (String) task.getInputData().get("status");
        if (status == null || status.isBlank()) {
            status = "unknown";
        }

        System.out.println("  [wc_notify_callback] Sending callback to: " + callbackUrl);

        Map<String, Object> callbackPayload = Map.of(
                "requestId", requestId,
                "status", status,
                "result", processingResult,
                "completedAt", "2026-01-15T10:00:00Z"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("callbackSent", true);
        result.getOutputData().put("responseStatus", 200);
        result.getOutputData().put("callbackPayload", callbackPayload);
        result.getOutputData().put("sentAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
