package tooluselogging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Creates an audit trail entry for a tool invocation.
 * Takes requestId, userId, sessionId, toolName, toolArgs, result, executionTimeMs,
 * requestTimestamp, responseTimestamp and returns an audit entry with compliance data.
 */
public class CreateAuditEntryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tl_create_audit_entry";
    }

    @Override
    public TaskResult execute(Task task) {
        String requestId = (String) task.getInputData().get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req-unknown";
        }

        String userId = (String) task.getInputData().get("userId");
        if (userId == null || userId.isBlank()) {
            userId = "anonymous";
        }

        String sessionId = (String) task.getInputData().get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "no-session";
        }

        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        Object toolArgs = task.getInputData().get("toolArgs");
        Object resultData = task.getInputData().get("result");

        Object executionTimeMsObj = task.getInputData().get("executionTimeMs");
        int executionTimeMs = 0;
        if (executionTimeMsObj instanceof Number) {
            executionTimeMs = ((Number) executionTimeMsObj).intValue();
        }

        String requestTimestamp = (String) task.getInputData().get("requestTimestamp");
        if (requestTimestamp == null || requestTimestamp.isBlank()) {
            requestTimestamp = "unknown";
        }

        String responseTimestamp = (String) task.getInputData().get("responseTimestamp");
        if (responseTimestamp == null || responseTimestamp.isBlank()) {
            responseTimestamp = "unknown";
        }

        System.out.println("  [tl_create_audit_entry] Creating audit entry for requestId: " + requestId + " (user=" + userId + ")");

        String auditId = "aud-fixed-001";

        String summary = "Tool '" + toolName + "' executed successfully for user '" + userId
                + "' in session '" + sessionId + "' (requestId=" + requestId
                + ", executionTime=" + executionTimeMs + "ms)";

        Map<String, Object> compliance = Map.of(
                "gdprCompliant", true,
                "dataRetentionDays", 90,
                "piiDetected", false
        );

        Map<String, Object> metadata = Map.of(
                "auditId", auditId,
                "requestId", requestId,
                "userId", userId,
                "sessionId", sessionId,
                "toolName", toolName,
                "requestTimestamp", requestTimestamp,
                "responseTimestamp", responseTimestamp,
                "executionTimeMs", executionTimeMs
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("auditId", auditId);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("requestId", requestId);
        result.getOutputData().put("compliance", compliance);
        result.getOutputData().put("metadata", metadata);
        return result;
    }
}
