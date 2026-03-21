package scheduledevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteEventWorkerTest {

    private final ExecuteEventWorker worker = new ExecuteEventWorker();

    @Test
    void taskDefName() {
        assertEquals("se_execute_event", worker.getTaskDefName());
    }

    @Test
    void executesEventSuccessfully() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "payload", Map.of("action", "send-report")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("executedAt"));
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsExecutedAt() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "payload", Map.of("key", "value")));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("executedAt"));
    }

    @Test
    void outputContainsResultSuccess() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void handlesComplexPayload() {
        Task task = taskWith(Map.of(
                "eventId", "evt-004",
                "payload", Map.of(
                        "action", "cleanup",
                        "target", "temp-files",
                        "dryRun", false)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("executedAt"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-005");
        input.put("payload", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("executedAt"));
        assertEquals("success", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
