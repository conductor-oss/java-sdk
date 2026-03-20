package nestedsubworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChargeWorkerTest {

    private final ChargeWorker worker = new ChargeWorker();

    @Test
    void taskDefName() {
        assertEquals("nest_charge", worker.getTaskDefName());
    }

    @Test
    void chargesPaymentSuccessfully() {
        Task task = taskWith(Map.of("orderId", "ORD-5001", "amount", 149.99));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TXN-ORD-5001", result.getOutputData().get("transactionId"));
        assertEquals(true, result.getOutputData().get("charged"));
    }

    @Test
    void transactionIdIncludesOrderId() {
        Task task = taskWith(Map.of("orderId", "ORD-999", "amount", 50.0));
        TaskResult result = worker.execute(task);

        assertEquals("TXN-ORD-999", result.getOutputData().get("transactionId"));
    }

    @Test
    void handlesCustomOrderId() {
        Task task = taskWith(Map.of("orderId", "MY-ORDER-42", "amount", 200.0));
        TaskResult result = worker.execute(task);

        assertEquals("TXN-MY-ORDER-42", result.getOutputData().get("transactionId"));
        assertEquals(true, result.getOutputData().get("charged"));
    }

    @Test
    void defaultsOrderIdWhenMissing() {
        Task task = taskWith(Map.of("amount", 100.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TXN-UNKNOWN", result.getOutputData().get("transactionId"));
    }

    @Test
    void defaultsOrderIdWhenBlank() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "   ");
        input.put("amount", 100.0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("TXN-UNKNOWN", result.getOutputData().get("transactionId"));
    }

    @Test
    void defaultsOrderIdWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", null);
        input.put("amount", 100.0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("TXN-UNKNOWN", result.getOutputData().get("transactionId"));
    }

    @Test
    void handlesMissingAmount() {
        Task task = taskWith(Map.of("orderId", "ORD-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TXN-ORD-001", result.getOutputData().get("transactionId"));
    }

    @Test
    void handlesIntegerAmount() {
        Task task = taskWith(Map.of("orderId", "ORD-001", "amount", 200));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("charged"));
    }

    @Test
    void outputContainsExpectedFields() {
        Task task = taskWith(Map.of("orderId", "ORD-001", "amount", 100.0));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("transactionId"));
        assertTrue(result.getOutputData().containsKey("charged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
