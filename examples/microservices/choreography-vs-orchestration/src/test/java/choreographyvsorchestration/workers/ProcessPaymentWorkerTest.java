package choreographyvsorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessPaymentWorkerTest {

    @BeforeEach
    void resetProcessed() {
        ProcessPaymentWorker.getProcessedPayments().clear();
    }

    @Test
    void chargesSuccessfully() {
        ProcessPaymentWorker w = new ProcessPaymentWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "orderId", "ORD-001",
                "amount", 149.99,
                "customerId", "CUST-1"
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("charged"));
        assertEquals(false, r.getOutputData().get("duplicate"));
        assertNotNull(r.getOutputData().get("transactionId"));
        assertNotNull(r.getOutputData().get("idempotencyKey"));
    }

    @Test
    void detectsDuplicatePayment() {
        ProcessPaymentWorker w = new ProcessPaymentWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("orderId", "ORD-DUP", "amount", 50.0)));

        // First call
        TaskResult r1 = w.execute(t);
        assertEquals(false, r1.getOutputData().get("duplicate"));

        // Second call with same orderId + amount
        Task t2 = new Task();
        t2.setStatus(Task.Status.IN_PROGRESS);
        t2.setInputData(new HashMap<>(Map.of("orderId", "ORD-DUP", "amount", 50.0)));
        TaskResult r2 = w.execute(t2);
        assertEquals(true, r2.getOutputData().get("duplicate"));
    }

    @Test
    void failsWithMissingOrderId() {
        ProcessPaymentWorker w = new ProcessPaymentWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("amount", 100.0)));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("orderId"));
    }

    @Test
    void failsWithNegativeAmount() {
        ProcessPaymentWorker w = new ProcessPaymentWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("orderId", "ORD-NEG", "amount", -10.0)));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("positive"));
    }

    @Test
    void failsWithExcessiveAmount() {
        ProcessPaymentWorker w = new ProcessPaymentWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("orderId", "ORD-BIG", "amount", 100000.0)));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("50,000"));
    }
}
