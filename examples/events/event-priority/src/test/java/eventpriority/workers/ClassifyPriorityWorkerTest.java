package eventpriority.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyPriorityWorkerTest {

    private final ClassifyPriorityWorker worker = new ClassifyPriorityWorker();

    @Test
    void taskDefName() {
        assertEquals("pr_classify_priority", worker.getTaskDefName());
    }

    @Test
    void classifiesPaymentFailedAsHigh() {
        Task task = taskWith(Map.of("eventId", "evt-001", "eventType", "payment.failed"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("high", result.getOutputData().get("priority"));
        assertEquals("payment.failed", result.getOutputData().get("eventType"));
    }

    @Test
    void classifiesOrderCreatedAsMedium() {
        Task task = taskWith(Map.of("eventId", "evt-002", "eventType", "order.created"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("medium", result.getOutputData().get("priority"));
        assertEquals("order.created", result.getOutputData().get("eventType"));
    }

    @Test
    void classifiesUnknownEventAsLow() {
        Task task = taskWith(Map.of("eventId", "evt-003", "eventType", "inventory.low"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("low", result.getOutputData().get("priority"));
        assertEquals("inventory.low", result.getOutputData().get("eventType"));
    }

    @Test
    void classifiesEmptyStringAsLow() {
        Task task = taskWith(Map.of("eventId", "evt-004", "eventType", ""));
        TaskResult result = worker.execute(task);

        assertEquals("low", result.getOutputData().get("priority"));
        assertEquals("", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-005");
        input.put("eventType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("low", result.getOutputData().get("priority"));
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("low", result.getOutputData().get("priority"));
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @Test
    void outputAlwaysContainsPriority() {
        Task task = taskWith(Map.of("eventId", "evt-006", "eventType", "user.signup"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("priority"));
        assertNotNull(result.getOutputData().get("eventType"));
    }

    @Test
    void classifiesAnotherUnmappedEventAsLow() {
        Task task = taskWith(Map.of("eventId", "evt-007", "eventType", "notification.sent"));
        TaskResult result = worker.execute(task);

        assertEquals("low", result.getOutputData().get("priority"));
        assertEquals("notification.sent", result.getOutputData().get("eventType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
