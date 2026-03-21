package scheduledevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueueEventWorkerTest {

    private final QueueEventWorker worker = new QueueEventWorker();

    @Test
    void taskDefName() {
        assertEquals("se_queue_event", worker.getTaskDefName());
    }

    @Test
    void queuesEventSuccessfully() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "payload", Map.of("action", "send-report"),
                "scheduledTime", "2026-01-15T10:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("queued"));
        assertEquals("evt-001", result.getOutputData().get("eventId"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("scheduledTime"));
    }

    @Test
    void outputContainsQueuedFlag() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "payload", Map.of("key", "value"),
                "scheduledTime", "2026-02-01T08:00:00Z"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("queued"));
    }

    @Test
    void preservesScheduledTime() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "payload", Map.of(),
                "scheduledTime", "2026-06-15T14:30:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-06-15T14:30:00Z", result.getOutputData().get("scheduledTime"));
    }

    @Test
    void preservesEventId() {
        Task task = taskWith(Map.of(
                "eventId", "evt-custom-id",
                "payload", Map.of("x", 1),
                "scheduledTime", "2026-01-01T00:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals("evt-custom-id", result.getOutputData().get("eventId"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", Map.of());
        input.put("scheduledTime", "2026-01-15T10:00:00Z");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventId"));
    }

    @Test
    void handlesNullScheduledTime() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-004");
        input.put("payload", Map.of());
        input.put("scheduledTime", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("1970-01-01T00:00:00Z", result.getOutputData().get("scheduledTime"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-005");
        input.put("payload", null);
        input.put("scheduledTime", "2026-01-15T10:00:00Z");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("queued"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventId"));
        assertEquals("1970-01-01T00:00:00Z", result.getOutputData().get("scheduledTime"));
        assertEquals(true, result.getOutputData().get("queued"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
