package enterpriserag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.Map;

/**
 * Worker that creates a SOC2-compliant audit log entry for every query.
 * Takes userId, question, sessionId, source, answer, and tokensUsed.
 * Returns an auditEntry with timestamp and compliance status.
 */
public class AuditLogWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "er_audit_log";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        String question = (String) task.getInputData().get("question");
        String sessionId = (String) task.getInputData().get("sessionId");
        String source = (String) task.getInputData().get("source");
        Object answer = task.getInputData().get("answer");
        Object tokensUsed = task.getInputData().get("tokensUsed");

        System.out.println("  [audit_log] Logging query for user=" + userId
                + " source=" + source + " session=" + sessionId);

        Map<String, Object> auditEntry = Map.of(
                "timestamp", Instant.now().toString(),
                "userId", userId != null ? userId : "unknown",
                "question", question != null ? question : "",
                "sessionId", sessionId != null ? sessionId : "unknown",
                "source", source != null ? source : "unknown",
                "answer", answer != null ? answer : "",
                "tokensUsed", tokensUsed != null ? tokensUsed : 0,
                "compliance", "SOC2-logged"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("auditEntry", auditEntry);
        return result;
    }
}
