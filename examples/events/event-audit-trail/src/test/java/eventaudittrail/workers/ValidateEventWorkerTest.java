package eventaudittrail.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateEventWorkerTest {

    private final ValidateEventWorker worker = new ValidateEventWorker();

    @Test
    void taskDefName() {
        assertEquals("at_validate_event", worker.getTaskDefName());
    }

    @Test
    void validatesEvent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "eventData", Map.of("orderId", "ORD-100")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertEquals("evt-001", result.getOutputData().get("eventId"));
    }

    @Test
    void outputContainsValidTrue() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "eventData", Map.of("paymentId", "PAY-200")));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void outputContainsEventId() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "eventData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("evt-003", result.getOutputData().get("eventId"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("eventData", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventId"));
    }

    @Test
    void handlesNullEventData() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-004");
        input.put("eventData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertEquals("unknown", result.getOutputData().get("eventId"));
    }

    @Test
    void handlesComplexEventData() {
        Task task = taskWith(Map.of(
                "eventId", "evt-005",
                "eventData", Map.of("orderId", "ORD-555", "amount", 42.00, "items", 3)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertEquals("evt-005", result.getOutputData().get("eventId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
