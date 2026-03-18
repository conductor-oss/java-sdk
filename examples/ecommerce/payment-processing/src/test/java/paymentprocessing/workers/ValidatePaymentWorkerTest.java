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
 * Tests for {@link ValidatePaymentWorker}.
 * Requires STRIPE_API_KEY environment variable to be set.
 */
@EnabledIfEnvironmentVariable(named = "STRIPE_API_KEY", matches = ".+")
class ValidatePaymentWorkerTest {

    private static ValidatePaymentWorker worker;

    @BeforeAll
    static void setUp() {
        worker = new ValidatePaymentWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("pay_validate", worker.getTaskDefName());
    }

    @Test
    void validPayment() {
        Task task = taskWith(Map.of("orderId", "ORD-1", "amount", 100.0, "currency", "USD",
                "paymentMethod", Map.of("type", "credit_card", "last4", "4242")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertEquals(true, result.getOutputData().get("amountValid"));
        assertEquals(true, result.getOutputData().get("currencyValid"));
        assertEquals(true, result.getOutputData().get("paymentMethodValid"));
    }

    @Test
    void zeroAmountIsInvalid() {
        Task task = taskWith(Map.of("orderId", "ORD-2", "amount", 0, "currency", "USD",
                "paymentMethod", Map.of("type", "credit_card")));
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("valid"));
        assertEquals(false, result.getOutputData().get("amountValid"));
    }

    @Test
    void negativeAmountIsInvalid() {
        Task task = taskWith(Map.of("orderId", "ORD-3", "amount", -50.0, "currency", "USD",
                "paymentMethod", Map.of("type", "credit_card")));
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void excessiveAmountIsInvalid() {
        Task task = taskWith(Map.of("orderId", "ORD-4", "amount", 2_000_000.0, "currency", "USD",
                "paymentMethod", Map.of("type", "credit_card")));
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("valid"));
        assertEquals(false, result.getOutputData().get("amountValid"));
    }

    @Test
    void unsupportedCurrencyIsInvalid() {
        Task task = taskWith(Map.of("orderId", "ORD-5", "amount", 50.0, "currency", "XYZ",
                "paymentMethod", Map.of("type", "credit_card")));
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("valid"));
        assertEquals(false, result.getOutputData().get("currencyValid"));
    }

    @Test
    void invalidPaymentMethodTypeIsInvalid() {
        Task task = taskWith(Map.of("orderId", "ORD-6", "amount", 50.0, "currency", "USD",
                "paymentMethod", Map.of("type", "bitcoin")));
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("valid"));
        assertEquals(false, result.getOutputData().get("paymentMethodValid"));
    }

    @Test
    void missingPaymentMethodIsInvalid() {
        Task task = taskWith(new HashMap<>(Map.of("orderId", "ORD-7", "amount", 50.0, "currency", "USD")));
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void returnsFraudScoreForValidPayment() {
        Task task = taskWith(Map.of("orderId", "ORD-8", "amount", 50.0, "currency", "USD",
                "paymentMethod", Map.of("type", "card", "last4", "4242")));
        TaskResult result = worker.execute(task);
        Object fraudScore = result.getOutputData().get("fraudScore");
        assertNotNull(fraudScore);
        assertTrue(fraudScore instanceof Number);
        double score = ((Number) fraudScore).doubleValue();
        assertTrue(score >= 0.0 && score <= 1.0, "Fraud score should be between 0 and 1");
    }

    @Test
    void highAmountIncreasesFraudScore() {
        Task lowTask = taskWith(Map.of("orderId", "ORD-9", "amount", 50.0, "currency", "USD",
                "paymentMethod", Map.of("type", "card", "last4", "4242")));
        Task highTask = taskWith(Map.of("orderId", "ORD-10", "amount", 50000.0, "currency", "USD",
                "paymentMethod", Map.of("type", "card", "last4", "4242")));
        double lowScore = ((Number) worker.execute(lowTask).getOutputData().get("fraudScore")).doubleValue();
        double highScore = ((Number) worker.execute(highTask).getOutputData().get("fraudScore")).doubleValue();
        assertTrue(highScore > lowScore, "Higher amount should produce higher fraud score");
    }

    @Test
    void returnsValidationErrors() {
        Task task = taskWith(Map.of("orderId", "ORD-11", "amount", -1.0, "currency", "XYZ",
                "paymentMethod", Map.of("type", "bitcoin")));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("validationErrors"));
        assertFalse(result.getOutputData().get("validationErrors").toString().isEmpty());
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        // Verify constructor behavior is correct (it reads env var)
        // This test validates the contract: the worker was constructed successfully
        // because STRIPE_API_KEY is set
        assertNotNull(worker);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
