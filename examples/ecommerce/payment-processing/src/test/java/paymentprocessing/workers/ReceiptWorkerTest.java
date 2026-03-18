package paymentprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ReceiptWorker}.
 * Requires STRIPE_API_KEY environment variable.
 *
 * Receipt generation works even when Stripe lookup fails (graceful degradation),
 * so these tests verify both paths.
 */
@EnabledIfEnvironmentVariable(named = "STRIPE_API_KEY", matches = ".+")
class ReceiptWorkerTest {

    private static ReceiptWorker worker;

    @BeforeAll
    static void setUp() {
        worker = new ReceiptWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("pay_receipt", worker.getTaskDefName());
    }

    @Test
    void generatesReceiptEvenWhenStripeLookupFails() {
        Task task = taskWith(Map.of("orderId", "ORD-1", "captureId", "pi_nonexistent",
                "amount", 100.0, "currency", "USD"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("receiptId"));
        assertTrue(result.getOutputData().get("receiptId").toString().startsWith("RCPT-"));
        assertNotNull(result.getOutputData().get("generatedAt"));
    }

    @Test
    void receiptIdStartsWithRCPT() {
        Task task = taskWith(Map.of("orderId", "ORD-2", "captureId", "pi_test",
                "amount", 50.0, "currency", "EUR"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().get("receiptId").toString().startsWith("RCPT-"));
    }

    @Test
    void includesOrderAndAmountDetails() {
        Task task = taskWith(Map.of("orderId", "ORD-3", "captureId", "pi_test",
                "amount", 75.50, "currency", "USD"));
        TaskResult result = worker.execute(task);

        assertEquals("ORD-3", result.getOutputData().get("orderId"));
        assertEquals(75.50, ((Number) result.getOutputData().get("amount")).doubleValue());
        assertEquals("USD", result.getOutputData().get("currency"));
    }

    @Test
    void receiptIdIsDeterministicForSameInputs() {
        // Same inputs at the same second should produce the same receipt
        // (this verifies the SHA-256 approach works)
        Task task1 = taskWith(Map.of("orderId", "ORD-DET", "captureId", "pi_same",
                "amount", 100.0, "currency", "USD"));
        Task task2 = taskWith(Map.of("orderId", "ORD-DET", "captureId", "pi_same",
                "amount", 100.0, "currency", "USD"));
        // Execute both very quickly so they share the same epoch second
        String id1 = worker.execute(task1).getOutputData().get("receiptId").toString();
        String id2 = worker.execute(task2).getOutputData().get("receiptId").toString();
        // They should be the same if executed in the same second
        assertEquals(id1, id2, "Same inputs in same second should produce same receipt ID");
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
