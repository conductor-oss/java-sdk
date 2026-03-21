package delayedevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveEventWorkerTest {

    private final ReceiveEventWorker worker = new ReceiveEventWorker();

    @Test
    void taskDefName() {
        assertEquals("de_receive_event", worker.getTaskDefName());
    }

    @Test
    void receivesEventSuccessfully() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "payload", Map.of("type", "reminder", "message", "Follow up")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("received"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("receivedAt"));
    }

    @Test
    void outputContainsReceivedFlag() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "payload", Map.of("key", "value")));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("received"));
    }

    @Test
    void outputContainsReceivedAtTimestamp() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("receivedAt"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("receivedAt"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", Map.of("type", "test"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("received"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-004");
        input.put("payload", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("received"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("received"));
        assertNotNull(result.getOutputData().get("receivedAt"));
    }

    @Test
    void handlesComplexPayload() {
        Map<String, Object> payload = Map.of(
                "type", "reminder",
                "message", "Follow up with customer",
                "priority", "high");
        Task task = taskWith(Map.of(
                "eventId", "evt-005",
                "payload", payload));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("received"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
