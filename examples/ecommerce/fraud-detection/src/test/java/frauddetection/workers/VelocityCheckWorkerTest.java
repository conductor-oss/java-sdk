package frauddetection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link VelocityCheckWorker} -- verifies velocity detection and input validation.
 */
@SuppressWarnings("unchecked")
class VelocityCheckWorkerTest {

    @BeforeEach
    void setUp() {
        VelocityCheckWorker.resetState();
    }

    private final VelocityCheckWorker worker = new VelocityCheckWorker();

    @Test
    void taskDefName() {
        assertEquals("frd_velocity_check", worker.getTaskDefName());
    }

    @Test
    void singleTransactionIsNormal() {
        TaskResult result = execute("CUST-1", "TXN-1");
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("normal", result.getOutputData().get("velocityResult"));
    }

    @Test
    void failsWithTerminalErrorOnMissingCustomerId() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("transactionId", "TXN-1")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("customerId"));
    }

    @Test
    void failsWithTerminalErrorOnMissingTransactionId() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("customerId", "CUST-1")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("transactionId"));
    }

    @Test
    void failsWithTerminalErrorOnBlankCustomerId() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("customerId", "  ", "transactionId", "TXN-1")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void includesVelocityFlags() {
        TaskResult result = execute("CUST-2", "TXN-2");
        Map<String, Object> flags = (Map<String, Object>) result.getOutputData().get("velocityFlags");
        assertNotNull(flags);
        assertNotNull(flags.get("rapidSuccession"));
        assertNotNull(flags.get("unusualVolume"));
        assertNotNull(flags.get("dailySpike"));
        assertNotNull(flags.get("geographicAnomaly"));
    }

    private TaskResult execute(String customerId, String transactionId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("customerId", customerId, "transactionId", transactionId)));
        return worker.execute(task);
    }
}
