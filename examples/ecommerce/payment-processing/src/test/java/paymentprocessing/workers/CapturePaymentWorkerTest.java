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
 * Tests for {@link CapturePaymentWorker}.
 * Requires STRIPE_API_KEY environment variable.
 *
 * Note: Full capture tests require a confirmed PaymentIntent, which needs
 * a payment method attached. These tests verify error handling for uncapturable intents.
 */
@EnabledIfEnvironmentVariable(named = "STRIPE_API_KEY", matches = "sk_test_.+")
class CapturePaymentWorkerTest {

    private static CapturePaymentWorker worker;

    @BeforeAll
    static void setUp() {
        worker = new CapturePaymentWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("pay_capture", worker.getTaskDefName());
    }

    @Test
    void failsGracefullyForInvalidPaymentIntent() {
        Task task = taskWith(Map.of("authorizationId", "pi_nonexistent_12345", "amount", 100.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertNotNull(result.getOutputData().get("error"));
        assertEquals(false, result.getOutputData().get("captured"));
    }

    @Test
    void failsGracefullyForEmptyAuthorizationId() {
        Task task = taskWith(Map.of("authorizationId", "", "amount", 100.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("captured"));
    }

    @Test
    void includesErrorMessageOnFailure() {
        Task task = taskWith(Map.of("authorizationId", "pi_invalid", "amount", 50.0));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getReasonForIncompletion());
        assertTrue(result.getReasonForIncompletion().startsWith("Stripe capture failed:"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
