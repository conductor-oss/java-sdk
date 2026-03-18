package subworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChargePaymentWorkerTest {

    private final ChargePaymentWorker worker = new ChargePaymentWorker();

    @Test
    void taskDefName() {
        assertEquals("sub_charge_payment", worker.getTaskDefName());
    }

    @Test
    void chargesPaymentWithDeterministicTransactionId() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-001",
                "amount", 65.0,
                "paymentMethod", "credit_card"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TXN-ORD-001", result.getOutputData().get("transactionId"));
        assertEquals(true, result.getOutputData().get("charged"));
        assertEquals(65.0, result.getOutputData().get("amount"));
    }

    @Test
    void transactionIdMatchesOrderId() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-999",
                "amount", 100.0,
                "paymentMethod", "debit_card"));
        TaskResult result = worker.execute(task);

        assertEquals("TXN-ORD-999", result.getOutputData().get("transactionId"));
    }

    @Test
    void defaultsOrderIdWhenMissing() {
        Task task = taskWith(Map.of("amount", 50.0, "paymentMethod", "cash"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TXN-UNKNOWN", result.getOutputData().get("transactionId"));
    }

    @Test
    void defaultsOrderIdWhenBlank() {
        Task task = taskWith(new HashMap<>(Map.of(
                "orderId", "   ",
                "amount", 50.0,
                "paymentMethod", "cash")));
        TaskResult result = worker.execute(task);

        assertEquals("TXN-UNKNOWN", result.getOutputData().get("transactionId"));
    }

    @Test
    void handlesIntegerAmount() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-INT",
                "amount", 100,
                "paymentMethod", "credit_card"));
        TaskResult result = worker.execute(task);

        assertEquals(100.0, result.getOutputData().get("amount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
