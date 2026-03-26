package eventdrivenworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyEventWorkerTest {

    private final ClassifyEventWorker worker = new ClassifyEventWorker();

    @Test
    void taskDefName() {
        assertEquals("ed_classify_event", worker.getTaskDefName());
    }

    @Test
    void classifiesOrderCreated() {
        Task task = taskWith(Map.of("eventType", "order.created", "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order", result.getOutputData().get("category"));
        assertEquals("normal", result.getOutputData().get("priority"));
    }

    @Test
    void classifiesOrderUpdated() {
        Task task = taskWith(Map.of("eventType", "order.updated", "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("order", result.getOutputData().get("category"));
        assertEquals("normal", result.getOutputData().get("priority"));
    }

    @Test
    void classifiesPaymentReceived() {
        Task task = taskWith(Map.of("eventType", "payment.received", "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("payment", result.getOutputData().get("category"));
        assertEquals("high", result.getOutputData().get("priority"));
    }

    @Test
    void classifiesPaymentRefunded() {
        Task task = taskWith(Map.of("eventType", "payment.refunded", "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("payment", result.getOutputData().get("category"));
        assertEquals("high", result.getOutputData().get("priority"));
    }

    @Test
    void classifiesUnknownEventAsGeneric() {
        Task task = taskWith(Map.of("eventType", "inventory.low", "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("generic", result.getOutputData().get("category"));
        assertEquals("normal", result.getOutputData().get("priority"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", null);
        input.put("metadata", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("generic", result.getOutputData().get("category"));
        assertEquals("normal", result.getOutputData().get("priority"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("generic", result.getOutputData().get("category"));
        assertEquals("normal", result.getOutputData().get("priority"));
    }

    @Test
    void classifiesEmptyStringAsGeneric() {
        Task task = taskWith(Map.of("eventType", "", "metadata", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("generic", result.getOutputData().get("category"));
        assertEquals("normal", result.getOutputData().get("priority"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
