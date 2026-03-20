package scheduledevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmWorkerTest {

    private final ConfirmWorker worker = new ConfirmWorker();

    @Test
    void taskDefName() {
        assertEquals("se_confirm", worker.getTaskDefName());
    }

    @Test
    void confirmsSuccessfully() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "executedAt", "2026-01-15T10:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void outputContainsConfirmedFlag() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "executedAt", "2026-02-01T08:00:00Z"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("confirmed"));
    }

    @Test
    void handlesVariousTimestamps() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "executedAt", "2026-12-31T23:59:59Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("executedAt", "2026-01-15T10:00:00Z");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void handlesNullExecutedAt() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-004");
        input.put("executedAt", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void handlesDifferentEventIds() {
        Task task = taskWith(Map.of(
                "eventId", "evt-special-999",
                "executedAt", "2026-01-15T10:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue((Boolean) result.getOutputData().get("confirmed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
