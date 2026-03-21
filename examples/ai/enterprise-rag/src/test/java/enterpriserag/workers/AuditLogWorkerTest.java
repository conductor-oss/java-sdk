package enterpriserag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogWorkerTest {

    private final AuditLogWorker worker = new AuditLogWorker();

    @Test
    void taskDefName() {
        assertEquals("er_audit_log", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsAuditEntry() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", "user-42");
        input.put("question", "What is RAG?");
        input.put("sessionId", "sess-abc-123");
        input.put("source", "generated");
        input.put("answer", "RAG is a technique...");
        input.put("tokensUsed", 187);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> auditEntry =
                (Map<String, Object>) result.getOutputData().get("auditEntry");
        assertNotNull(auditEntry);
        assertEquals("user-42", auditEntry.get("userId"));
        assertEquals("What is RAG?", auditEntry.get("question"));
        assertEquals("sess-abc-123", auditEntry.get("sessionId"));
        assertEquals("generated", auditEntry.get("source"));
        assertEquals("SOC2-logged", auditEntry.get("compliance"));
        assertNotNull(auditEntry.get("timestamp"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesNullInputsGracefully() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", null);
        input.put("question", null);
        input.put("sessionId", null);
        input.put("source", null);
        input.put("answer", null);
        input.put("tokensUsed", null);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> auditEntry =
                (Map<String, Object>) result.getOutputData().get("auditEntry");
        assertNotNull(auditEntry);
        assertEquals("unknown", auditEntry.get("userId"));
        assertEquals("SOC2-logged", auditEntry.get("compliance"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
