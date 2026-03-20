package eventdrivenmicroservices.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceWorkerTest {

    private final PaymentServiceWorker worker = new PaymentServiceWorker();

    @Test
    void taskDefName() {
        assertEquals("dm_payment_service", worker.getTaskDefName());
    }

    @Test
    void processesPayment() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-DM-1001",
                "amount", 199.96,
                "paymentMethod", "credit_card"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TXN-fixed-001", result.getOutputData().get("transactionId"));
        assertEquals("charged", result.getOutputData().get("status"));
    }

    @Test
    void outputContainsAmount() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-100",
                "amount", 50.0,
                "paymentMethod", "debit_card"));
        TaskResult result = worker.execute(task);

        assertEquals(50.0, ((Number) result.getOutputData().get("amount")).doubleValue(), 0.01);
    }

    @Test
    void outputContainsTransactionId() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-200",
                "amount", 75.0,
                "paymentMethod", "paypal"));
        TaskResult result = worker.execute(task);

        assertEquals("TXN-fixed-001", result.getOutputData().get("transactionId"));
    }

    @Test
    void outputStatusIsCharged() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-300",
                "amount", 100.0,
                "paymentMethod", "credit_card"));
        TaskResult result = worker.execute(task);

        assertEquals("charged", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullPaymentMethod() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-400");
        input.put("amount", 25.0);
        input.put("paymentMethod", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TXN-fixed-001", result.getOutputData().get("transactionId"));
    }

    @Test
    void handlesNullAmount() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-500");
        input.put("amount", null);
        input.put("paymentMethod", "credit_card");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("amount"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TXN-fixed-001", result.getOutputData().get("transactionId"));
        assertEquals("charged", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
