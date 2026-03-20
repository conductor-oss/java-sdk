package tooluselogging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Logs an incoming tool request.
 * Takes toolName, toolArgs, userId, sessionId and returns a fixed requestId, timestamp, and log entry.
 */
public class LogRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tl_log_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        Object toolArgs = task.getInputData().get("toolArgs");
        String userId = (String) task.getInputData().get("userId");
        if (userId == null || userId.isBlank()) {
            userId = "anonymous";
        }

        String sessionId = (String) task.getInputData().get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "no-session";
        }

        System.out.println("  [tl_log_request] Logging request for tool: " + toolName + " (user=" + userId + ")");

        String requestId = "req-fixed-abc123";
        String timestamp = "2026-03-08T10:00:00Z";

        Map<String, Object> logEntry = Map.of(
                "requestId", requestId,
                "toolName", toolName,
                "userId", userId,
                "sessionId", sessionId,
                "timestamp", timestamp,
                "direction", "request"
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
