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
 * Tests for {@link AuthorizePaymentWorker}.
 * Requires STRIPE_API_KEY environment variable (sk_test_ key).
 * These tests create real PaymentIntents in Stripe test mode.
 */
@EnabledIfEnvironmentVariable(named = "STRIPE_API_KEY", matches = "sk_test_.+")
class AuthorizePaymentWorkerTest {

    private static AuthorizePaymentWorker worker;

    @BeforeAll
    static void setUp() {
        worker = new AuthorizePaymentWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("pay_authorize", worker.getTaskDefName());
    }

    @Test
    void createsRealPaymentIntent() {
        Task task = taskWith(Map.of("orderId", "ORD-TEST-1", "amount", 100.0,
                "paymentMethod", Map.of("type", "card")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().get("authorizationId").toString().startsWith("pi_"),
                "Should return a real Stripe PaymentIntent ID");
        assertEquals(true, result.getOutputData().get("authorized"));
        assertEquals("requires_payment_method", result.getOutputData().get("stripeStatus"));
        assertNotNull(result.getOutputData().get("clientSecret"));
        assertNotNull(result.getOutputData().get("expiresAt"));
    }

    @Test
    void authorizationIdIsUnique() {
        Task t1 = taskWith(Map.of("orderId", "ORD-A", "amount", 1.0,
                "paymentMethod", Map.of("type", "card")));
        Task t2 = taskWith(Map.of("orderId", "ORD-B", "amount", 2.0,
                "paymentMethod", Map.of("type", "card")));
        String id1 = worker.execute(t1).getOutputData().get("authorizationId").toString();
        String id2 = worker.execute(t2).getOutputData().get("authorizationId").toString();
        assertNotEquals(id1, id2, "Each PaymentIntent should have a unique ID");
    }

    @Test
    void amountConvertedToCents() {
        Task task = taskWith(Map.of("orderId", "ORD-TEST-2", "amount", 49.99,
                "paymentMethod", Map.of("type", "card")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4999L, ((Number) result.getOutputData().get("amountInCents")).longValue());
    }

    @Test
    void defaultCurrencyIsUsd() {
        Task task = taskWith(Map.of("orderId", "ORD-TEST-3", "amount", 10.0,
                "paymentMethod", Map.of("type", "card")));
        TaskResult result = worker.execute(task);

        assertEquals("usd", result.getOutputData().get("currency"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
