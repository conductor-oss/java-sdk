package eventchoreography.workers;

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
        assertEquals("ch_payment_service", worker.getTaskDefName());
    }

    @Test
    void processesPayment() {
        Task task = taskWith(Map.of("orderId", "ORD-100", "amount", 79.98));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("paid"));
        assertEquals("txn_ch_fixed_001", result.getOutputData().get("transactionId"));
    }

    @Test
    void outputContainsPaidTrue() {
        Task task = taskWith(Map.of("orderId", "ORD-200", "amount", 100.0));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("paid"));
    }

    @Test
    void outputContainsTransactionId() {
        Task task = taskWith(Map.of("orderId", "ORD-300", "amount", 50.0));
        TaskResult result = worker.execute(task);

        assertEquals("txn_ch_fixed_001", result.getOutputData().get("transactionId"));
    }

    @Test
    void handlesNullOrderId() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", null);
        input.put("amount", 25.0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("paid"));
    }

    @Test
    void handlesNullAmount() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-400");
        input.put("amount", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("txn_ch_fixed_001", result.getOutputData().get("transactionId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("paid"));
    }

    @Test
    void transactionIdIsDeterministic() {
        Task task1 = taskWith(Map.of("orderId", "ORD-A", "amount", 10.0));
        Task task2 = taskWith(Map.of("orderId", "ORD-B", "amount", 20.0));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("transactionId"),
                     result2.getOutputData().get("transactionId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
