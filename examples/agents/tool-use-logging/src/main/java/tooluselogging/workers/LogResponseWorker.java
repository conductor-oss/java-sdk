package tooluselogging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Logs a tool response.
 * Takes requestId, toolName, result, executionTimeMs, toolStatus and returns a fixed timestamp and log entry.
 */
public class LogResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tl_log_response";
    }

    @Override
    public TaskResult execute(Task task) {
        String requestId = (String) task.getInputData().get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req-unknown";
        }

        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        Object resultData = task.getInputData().get("result");

        Object executionTimeMsObj = task.getInputData().get("executionTimeMs");
        int executionTimeMs = 0;
        if (executionTimeMsObj instanceof Number) {
            executionTimeMs = ((Number) executionTimeMsObj).intValue();
        }

        String toolStatus = (String) task.getInputData().get("toolStatus");
        if (toolStatus == null || toolStatus.isBlank()) {
            toolStatus = "unknown";
        }

        System.out.println("  [tl_log_response] Logging response for requestId: " + requestId + " (status=" + toolStatus + ")");

        String timestamp = "2026-03-08T10:00:01Z";

        Map<String, Object> logEntry = Map.of(
                "requestId", requestId,
                "toolName", toolName,
                "toolStatus", toolStatus,
                "executionTimeMs", executionTimeMs,
                "timestamp", timestamp,
                "direction", "response"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestId", requestId);
        result.getOutputData().put("timestamp", timestamp);
        result.getOutputData().put("logged", true);
        result.getOutputData().put("logEntry", logEntry);
        return result;
    }
}
