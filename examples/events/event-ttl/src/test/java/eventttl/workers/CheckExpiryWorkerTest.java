package eventttl.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckExpiryWorkerTest {

    private final CheckExpiryWorker worker = new CheckExpiryWorker();

    @Test
    void taskDefName() {
        assertEquals("xl_check_expiry", worker.getTaskDefName());
    }

    @Test
    void returnsValidStatusForEventWithinTtl() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "ttlSeconds", 600,
                "createdAt", "2026-01-15T09:58:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("valid", result.getOutputData().get("status"));
        assertEquals(120, result.getOutputData().get("ageSeconds"));
        assertEquals(600, result.getOutputData().get("ttlSeconds"));
    }

    @Test
    void alwaysReturnsAgeSeconds120() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "ttlSeconds", 300,
                "createdAt", "2025-01-01T00:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(120, result.getOutputData().get("ageSeconds"));
    }

    @Test
    void alwaysReturnsStatusValid() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "ttlSeconds", 10,
                "createdAt", "2020-01-01T00:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals("valid", result.getOutputData().get("status"));
    }

    @Test
    void parsesTtlFromStringValue() {
        Task task = taskWith(Map.of(
                "eventId", "evt-004",
                "ttlSeconds", "450",
                "createdAt", "2026-01-15T10:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(450, result.getOutputData().get("ttlSeconds"));
    }

    @Test
    void defaultsTtlTo300WhenMissing() {
        Task task = taskWith(Map.of(
                "eventId", "evt-005",
                "createdAt", "2026-01-15T10:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(300, result.getOutputData().get("ttlSeconds"));
    }

    @Test
    void handlesNullTtlSeconds() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-006");
        input.put("ttlSeconds", null);
        input.put("createdAt", "2026-01-15T10:00:00Z");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(300, result.getOutputData().get("ttlSeconds"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("ttlSeconds", 600);
        input.put("createdAt", "2026-01-15T10:00:00Z");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("valid", result.getOutputData().get("status"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("valid", result.getOutputData().get("status"));
        assertEquals(120, result.getOutputData().get("ageSeconds"));
        assertEquals(300, result.getOutputData().get("ttlSeconds"));
    }

    @Test
    void handlesInvalidTtlString() {
        Task task = taskWith(Map.of(
                "eventId", "evt-008",
                "ttlSeconds", "not-a-number",
                "createdAt", "2026-01-15T10:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(300, result.getOutputData().get("ttlSeconds"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
