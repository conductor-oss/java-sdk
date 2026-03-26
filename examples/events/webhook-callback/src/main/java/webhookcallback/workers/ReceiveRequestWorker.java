package webhookcallback.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives an incoming webhook request, validates it, and parses the payload
 * into a structured format for downstream processing.
 */
public class ReceiveRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wc_receive_request";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String requestId = (String) task.getInputData().get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = "unknown";
        }

        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("data");
        if (data == null) {
            data = Map.of();
        }

        System.out.println("  [wc_receive_request] Receiving request: " + requestId);

        String type = data.get("type") != null ? data.get("type").toString() : "unknown";
        int recordCount = 0;
        if (data.get("recordCount") != null) {
            recordCount = ((Number) data.get("recordCount")).intValue();
        }

        Map<String, Object> parsedData = Map.of(
                "type", type,
                "records", recordCount,
                "format", "json",
                "sizeBytes", 48200
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestId", requestId);
        result.getOutputData().put("parsedData", parsedData);
        result.getOutputData().put("receivedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
