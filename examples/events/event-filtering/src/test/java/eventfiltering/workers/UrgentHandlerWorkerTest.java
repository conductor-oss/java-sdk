package eventfiltering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UrgentHandlerWorkerTest {

    private final UrgentHandlerWorker worker = new UrgentHandlerWorker();

    @Test
    void taskDefName() {
        assertEquals("ef_urgent_handler", worker.getTaskDefName());
    }

    @Test
    void handlesUrgentEvent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "payload", Map.of("service", "payment-gateway"),
                "priority", "urgent"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("handled"));
    }

    @Test
    void setsHandlerToUrgent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "payload", Map.of(),
                "priority", "urgent"));
        TaskResult result = worker.execute(task);

        assertEquals("urgent", result.getOutputData().get("handler"));
    }

    @Test
    void setsAlertSent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "payload", Map.of(),
                "priority", "urgent"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("alertSent"));
    }

    @Test
    void setsEscalated() {
        Task task = taskWith(Map.of(
                "eventId", "evt-004",
                "payload", Map.of(),
                "priority", "urgent"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("escalated"));
    }

    @Test
    void setsProcessedAt() {
        Task task = taskWith(Map.of(
                "eventId", "evt-005",
                "payload", Map.of(),
                "priority", "urgent"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("processedAt"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", Map.of());
        input.put("priority", "urgent");
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
        assertEquals("urgent", result.getOutputData().get("handler"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
