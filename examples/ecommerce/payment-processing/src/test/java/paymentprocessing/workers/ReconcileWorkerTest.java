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
 * Tests for {@link ReconcileWorker}.
 * Requires STRIPE_API_KEY environment variable.
 *
 * Tests verify error handling for invalid PaymentIntent IDs.
 */
@EnabledIfEnvironmentVariable(named = "STRIPE_API_KEY", matches = ".+")
class ReconcileWorkerTest {

    private static ReconcileWorker worker;

    @BeforeAll
    static void setUp() {
        worker = new ReconcileWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("pay_reconcile", worker.getTaskDefName());
    }

    @Test
    void failsForInvalidCaptureId() {
        Task task = taskWith(Map.of("captureId", "pi_nonexistent", "merchantId", "merch-1", "amount", 100.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("reconciled"));
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void failsGracefullyForEmptyCaptureId() {
        Task task = taskWith(Map.of("captureId", "", "merchantId", "merch-2", "amount", 50.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("reconciled"));
    }

    @Test
    void includesReasonForIncompletion() {
        Task task = taskWith(Map.of("captureId", "pi_bad", "merchantId", "merch-3", "amount", 75.0));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getReasonForIncompletion());
        assertTrue(result.getReasonForIncompletion().contains("Reconciliation failed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
