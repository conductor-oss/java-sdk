package checkoutflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessPaymentWorkerTest {

    private ProcessPaymentWorker worker;

    @BeforeEach
    void setUp() {
        ProcessPaymentWorker.resetState();
        worker = new ProcessPaymentWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("chk_process_payment", worker.getTaskDefName());
    }

    @Test
    void returnsPaymentId() {
        Task task = taskWith(Map.of("userId", "usr-1", "amount", 100.0,
                "paymentMethod", Map.of("type", "credit_card", "last4", "4242")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("paymentId"));
        assertTrue(result.getOutputData().get("paymentId").toString().startsWith("pay-"));
    }

    @Test
    void returnsCapturedStatus() {
        Task task = taskWith(Map.of("userId", "usr-2", "amount", 50.0,
                "paymentMethod", Map.of("type", "debit_card")));
        TaskResult result = worker.execute(task);

        assertEquals("captured", result.getOutputData().get("status"));
    }

    @Test
    void returnsChargedAt() {
        Task task = taskWith(Map.of("userId", "usr-3", "amount", 75.0,
                "paymentMethod", Map.of("type", "credit_card")));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("chargedAt"));
    }

    @Test
    void paymentIdIsUnique() {
        Task task1 = taskWith(Map.of("userId", "usr-1", "amount", 10.0,
                "paymentMethod", Map.of("type", "card")));
        Task task2 = taskWith(Map.of("userId", "usr-2", "amount", 20.0,
                "paymentMethod", Map.of("type", "card")));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertNotEquals(r1.getOutputData().get("paymentId"), r2.getOutputData().get("paymentId"));
    }

    @Test
    void rejectsZeroAmount() {
        Task task = taskWith(Map.of("userId", "usr-4", "amount", 0.0,
                "paymentMethod", Map.of("type", "card")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("failed", result.getOutputData().get("status"));
    }

    @Test
    void rejectsNegativeAmount() {
        Task task = taskWith(Map.of("userId", "usr-5", "amount", -10.0,
                "paymentMethod", Map.of("type", "card")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void rejectsUnsupportedPaymentType() {
        Task task = taskWith(Map.of("userId", "usr-6", "amount", 50.0,
                "paymentMethod", Map.of("type", "bitcoin")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void tracksPaymentType() {
        Task task = taskWith(Map.of("userId", "usr-7", "amount", 50.0,
                "paymentMethod", Map.of("type", "apple_pay")));
        TaskResult result = worker.execute(task);

        assertEquals("apple_pay", result.getOutputData().get("paymentType"));
    }

    @Test
    void duplicatePaymentReturnsSameId() {
        Task task1 = taskWith(Map.of("userId", "usr-dup", "amount", 100.0,
                "paymentMethod", Map.of("type", "card")));
        Task task2 = taskWith(Map.of("userId", "usr-dup", "amount", 100.0,
                "paymentMethod", Map.of("type", "card")));

        String id1 = worker.execute(task1).getOutputData().get("paymentId").toString();
        TaskResult r2 = worker.execute(task2);
        assertEquals(id1, r2.getOutputData().get("paymentId"));
        assertEquals(true, r2.getOutputData().get("duplicate"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
