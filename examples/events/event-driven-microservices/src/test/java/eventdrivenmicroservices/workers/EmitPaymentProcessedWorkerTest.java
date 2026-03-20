package eventdrivenmicroservices.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmitPaymentProcessedWorkerTest {

    private final EmitPaymentProcessedWorker worker = new EmitPaymentProcessedWorker();

    @Test
    void taskDefName() {
        assertEquals("dm_emit_payment_processed", worker.getTaskDefName());
    }

    @Test
    void publishesPaymentProcessedEvent() {
        Task task = taskWith(Map.of(
                "eventType", "payment.processed",
                "orderId", "ORD-DM-1001",
                "transactionId", "TXN-fixed-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
        assertEquals("payment.processed", result.getOutputData().get("eventType"));
    }

    @Test
    void outputContainsPublishedTrue() {
        Task task = taskWith(Map.of(
                "eventType", "payment.processed",
                "orderId", "ORD-100",
                "transactionId", "TXN-100"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("published"));
    }

    @Test
    void outputContainsEventType() {
        Task task = taskWith(Map.of(
                "eventType", "payment.processed",
                "orderId", "ORD-200",
                "transactionId", "TXN-200"));
        TaskResult result = worker.execute(task);

        assertEquals("payment.processed", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesCustomEventType() {
        Task task = taskWith(Map.of(
                "eventType", "payment.refunded",
                "orderId", "ORD-300",
                "transactionId", "TXN-300"));
        TaskResult result = worker.execute(task);

        assertEquals("payment.refunded", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", null);
        input.put("orderId", "ORD-400");
        input.put("transactionId", "TXN-400");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("payment.processed", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullTransactionId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", "payment.processed");
        input.put("orderId", "ORD-500");
        input.put("transactionId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
        assertEquals("payment.processed", result.getOutputData().get("eventType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
