package eventdrivenworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HandlePaymentWorkerTest {

    private final HandlePaymentWorker worker = new HandlePaymentWorker();

    @Test
    void taskDefName() {
        assertEquals("ed_handle_payment", worker.getTaskDefName());
    }

    @Test
    void handlesPaymentWithPaymentId() {
        Task task = taskWith(Map.of(
                "eventData", Map.of("paymentId", "PAY-200", "amount", 150.00),
                "priority", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("payment", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("PAY-200", result.getOutputData().get("paymentId"));
    }

    @Test
    void handlesRefundedPayment() {
        Task task = taskWith(Map.of(
                "eventData", Map.of("paymentId", "PAY-REFUND-100"),
                "priority", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("payment", result.getOutputData().get("handler"));
        assertEquals("PAY-REFUND-100", result.getOutputData().get("paymentId"));
    }

    @Test
    void outputAlwaysHasHandlerPayment() {
        Task task = taskWith(Map.of(
                "eventData", Map.of("paymentId", "PAY-999"),
                "priority", "high"));
        TaskResult result = worker.execute(task);

        assertEquals("payment", result.getOutputData().get("handler"));
    }

    @Test
    void outputAlwaysHasProcessedTrue() {
        Task task = taskWith(Map.of(
                "eventData", Map.of("paymentId", "PAY-999"),
                "priority", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesEmptyEventData() {
        Task task = taskWith(Map.of(
                "eventData", Map.of(),
                "priority", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("paymentId"));
    }

    @Test
    void handlesNullPriority() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventData", Map.of("paymentId", "PAY-300"));
        input.put("priority", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("PAY-300", result.getOutputData().get("paymentId"));
    }

    @Test
    void handlesNullEventData() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventData", null);
        input.put("priority", "high");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("paymentId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("payment", result.getOutputData().get("handler"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
