package eventaudittrail.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogReceivedWorkerTest {

    private final LogReceivedWorker worker = new LogReceivedWorker();

    @Test
    void taskDefName() {
        assertEquals("at_log_received", worker.getTaskDefName());
    }

    @Test
    void logsReceivedEvent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "eventType", "order.created",
                "stage", "received"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("received", result.getOutputData().get("stage"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("timestamp"));
    }

    @Test
    void outputContainsLoggedTrue() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "eventType", "payment.received",
                "stage", "received"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void outputStageIsAlwaysReceived() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "eventType", "order.updated",
                "stage", "some-other-stage"));
        TaskResult result = worker.execute(task);

        assertEquals("received", result.getOutputData().get("stage"));
    }

    @Test
    void outputContainsFixedTimestamp() {
        Task task = taskWith(Map.of(
                "eventId", "evt-004",
                "eventType", "inventory.low",
                "stage", "received"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("timestamp"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("eventType", "order.created");
        input.put("stage", "received");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-005");
        input.put("eventType", null);
        input.put("stage", "received");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("received", result.getOutputData().get("stage"));
    }

    @Test
    void handlesNullStage() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-006");
        input.put("eventType", "order.created");
        input.put("stage", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("received", result.getOutputData().get("stage"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
