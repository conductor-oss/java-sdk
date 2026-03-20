package eventfiltering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StandardHandlerWorkerTest {

    private final StandardHandlerWorker worker = new StandardHandlerWorker();

    @Test
    void taskDefName() {
        assertEquals("ef_standard_handler", worker.getTaskDefName());
    }

    @Test
    void handlesStandardEvent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "payload", Map.of("service", "logging"),
                "priority", "standard"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("handled"));
    }

    @Test
    void setsHandlerToStandard() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "payload", Map.of(),
                "priority", "standard"));
        TaskResult result = worker.execute(task);

        assertEquals("standard", result.getOutputData().get("handler"));
    }

    @Test
    void setsQueued() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "payload", Map.of(),
                "priority", "standard"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("queued"));
    }

    @Test
    void setsProcessedAt() {
        Task task = taskWith(Map.of(
                "eventId", "evt-004",
                "payload", Map.of(),
                "priority", "standard"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("processedAt"));
    }

    @Test
    void doesNotSetAlertSent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-005",
                "payload", Map.of(),
                "priority", "standard"));
        TaskResult result = worker.execute(task);

        assertNull(result.getOutputData().get("alertSent"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", Map.of());
        input.put("priority", "standard");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("handled"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("handled"));
        assertEquals("standard", result.getOutputData().get("handler"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
