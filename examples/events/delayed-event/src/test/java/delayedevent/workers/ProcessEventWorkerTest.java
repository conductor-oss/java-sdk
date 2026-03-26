package delayedevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessEventWorkerTest {

    private final ProcessEventWorker worker = new ProcessEventWorker();

    @Test
    void taskDefName() {
        assertEquals("de_process_event", worker.getTaskDefName());
    }

    @Test
    void processesEventSuccessfully() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "payload", Map.of("type", "reminder", "message", "Follow up")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("2026-01-15T10:00:05Z", result.getOutputData().get("processedAt"));
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsProcessedAtTimestamp() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("processedAt"));
        assertEquals("2026-01-15T10:00:05Z", result.getOutputData().get("processedAt"));
    }

    @Test
    void outputContainsSuccessResult() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "payload", Map.of("key", "value")));
        TaskResult result = worker.execute(task);

        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", Map.of("type", "test"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-004");
        input.put("payload", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("2026-01-15T10:00:05Z", result.getOutputData().get("processedAt"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("2026-01-15T10:00:05Z", result.getOutputData().get("processedAt"));
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void handlesComplexPayload() {
        Map<String, Object> payload = Map.of(
                "type", "order",
                "orderId", "ORD-9921",
                "amount", 340.50);
        Task task = taskWith(Map.of(
                "eventId", "evt-005",
                "payload", payload));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
