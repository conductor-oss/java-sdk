package subworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidatePaymentWorkerTest {

    private final ValidatePaymentWorker worker = new ValidatePaymentWorker();

    @Test
    void taskDefName() {
        assertEquals("sub_validate_payment", worker.getTaskDefName());
    }

    @Test
    void validPayment() {
        Task task = taskWith(Map.of("paymentMethod", "credit_card", "amount", 65.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertEquals("Payment validated", result.getOutputData().get("reason"));
    }

    @Test
    void missingPaymentMethod() {
        Task task = taskWith(Map.of("amount", 65.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("valid"));
        assertEquals("Missing payment method", result.getOutputData().get("reason"));
    }

    @Test
    void blankPaymentMethod() {
        Task task = taskWith(new HashMap<>(Map.of("paymentMethod", "   ", "amount", 65.0)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("valid"));
        assertEquals("Missing payment method", result.getOutputData().get("reason"));
    }

    @Test
    void missingAmount() {
        Task task = taskWith(Map.of("paymentMethod", "credit_card"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("valid"));
        assertEquals("Missing amount", result.getOutputData().get("reason"));
    }

    @Test
    void zeroAmount() {
        Task task = taskWith(Map.of("paymentMethod", "credit_card", "amount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("valid"));
        assertTrue(((String) result.getOutputData().get("reason")).startsWith("Invalid amount"));
    }

    @Test
    void negativeAmount() {
        Task task = taskWith(Map.of("paymentMethod", "credit_card", "amount", -10.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("valid"));
        assertTrue(((String) result.getOutputData().get("reason")).startsWith("Invalid amount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
