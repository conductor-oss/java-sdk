package tooluselogging.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateAuditEntryWorkerTest {

    private final CreateAuditEntryWorker worker = new CreateAuditEntryWorker();

    @Test
    void taskDefName() {
        assertEquals("tl_create_audit_entry", worker.getTaskDefName());
    }

    @Test
    void returnsFixedAuditId() {
        Task task = taskWith(Map.of(
                "requestId", "req-fixed-abc123",
                "userId", "user-7392",
                "sessionId", "sess-abc-12345",
                "toolName", "sentiment_analysis",
                "executionTimeMs", 187,
                "requestTimestamp", "2026-03-08T10:00:00Z",
                "responseTimestamp", "2026-03-08T10:00:01Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("aud-fixed-001", result.getOutputData().get("auditId"));
    }

    @Test
    void returnsSummaryString() {
        Task task = taskWith(Map.of(
                "requestId", "req-fixed-abc123",
                "userId", "user-7392",
                "sessionId", "sess-abc-12345",
                "toolName", "sentiment_analysis",
                "executionTimeMs", 187,
                "requestTimestamp", "2026-03-08T10:00:00Z",
                "responseTimestamp", "2026-03-08T10:00:01Z"));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertTrue(summary.contains("sentiment_analysis"));
        assertTrue(summary.contains("user-7392"));
        assertTrue(summary.contains("sess-abc-12345"));
        assertTrue(summary.contains("req-fixed-abc123"));
    }

    @Test
    void returnsRequestId() {
        Task task = taskWith(Map.of(
                "requestId", "req-fixed-abc123",
                "userId", "user-1",
                "toolName", "test"));
        TaskResult result = worker.execute(task);

        assertEquals("req-fixed-abc123", result.getOutputData().get("requestId"));
    }

    @Test
    void returnsComplianceData() {
        Task task = taskWith(Map.of(
                "requestId", "req-1",
                "userId", "user-1",
                "toolName", "test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> compliance = (Map<String, Object>) result.getOutputData().get("compliance");
        assertNotNull(compliance);
        assertEquals(true, compliance.get("gdprCompliant"));
        assertEquals(90, compliance.get("dataRetentionDays"));
        assertEquals(false, compliance.get("piiDetected"));
    }

    @Test
    void returnsMetadataMap() {
        Task task = taskWith(Map.of(
                "requestId", "req-fixed-abc123",
                "userId", "user-7392",
                "sessionId", "sess-abc-12345",
                "toolName", "sentiment_analysis",
                "executionTimeMs", 187,
                "requestTimestamp", "2026-03-08T10:00:00Z",
                "responseTimestamp", "2026-03-08T10:00:01Z"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getOutputData().get("metadata");
        assertNotNull(metadata);
        assertEquals("aud-fixed-001", metadata.get("auditId"));
        assertEquals("req-fixed-abc123", metadata.get("requestId"));
        assertEquals("user-7392", metadata.get("userId"));
        assertEquals("sess-abc-12345", metadata.get("sessionId"));
        assertEquals("sentiment_analysis", metadata.get("toolName"));
        assertEquals(187, metadata.get("executionTimeMs"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("requestId", "req-1");
        input.put("userId", null);
        input.put("toolName", "test_tool");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("anonymous"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("auditId"));
        assertNotNull(result.getOutputData().get("summary"));
        assertNotNull(result.getOutputData().get("compliance"));
        assertNotNull(result.getOutputData().get("metadata"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
