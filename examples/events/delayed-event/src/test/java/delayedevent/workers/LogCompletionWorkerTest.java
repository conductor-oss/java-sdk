package delayedevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogCompletionWorkerTest {

    private final LogCompletionWorker worker = new LogCompletionWorker();

    @Test
    void taskDefName() {
        assertEquals("de_log_completion", worker.getTaskDefName());
    }

    @Test
    void logsCompletionSuccessfully() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "processedAt", "2026-01-15T10:00:05Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void outputContainsLoggedFlag() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "processedAt", "2026-01-15T10:00:10Z"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("logged"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("processedAt", "2026-01-15T10:00:05Z");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void handlesNullProcessedAt() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-003");
        input.put("processedAt", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void handlesVariousTimestamps() {
        Task task = taskWith(Map.of(
                "eventId", "evt-004",
                "processedAt", "2026-12-31T23:59:59Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void logsFlagIsAlwaysTrue() {
        Task task = taskWith(Map.of(
                "eventId", "evt-005",
                "processedAt", "2026-01-01T00:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals(1, result.getOutputData().size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
