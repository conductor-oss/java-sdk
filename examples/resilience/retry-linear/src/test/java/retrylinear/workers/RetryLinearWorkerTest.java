package retrylinear.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetryLinearWorkerTest {

    private RetryLinearWorker worker;

    @BeforeEach
    void setUp() {
        worker = new RetryLinearWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("retry_linear_task", worker.getTaskDefName());
    }

    @Test
    void failsFirstThreeAttempts() {
        Task task = taskWith(Map.of("service", "payment-gateway"));

        // Attempt 1 — should fail
        TaskResult result1 = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result1.getStatus());
        assertEquals(1, result1.getOutputData().get("attempts"));
        assertEquals("unavailable", result1.getOutputData().get("serviceStatus"));

        // Attempt 2 — should fail
        TaskResult result2 = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result2.getStatus());
        assertEquals(2, result2.getOutputData().get("attempts"));
        assertEquals("unavailable", result2.getOutputData().get("serviceStatus"));

        // Attempt 3 — should fail
        TaskResult result3 = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result3.getStatus());
        assertEquals(3, result3.getOutputData().get("attempts"));
        assertEquals("unavailable", result3.getOutputData().get("serviceStatus"));
    }

    @Test
    void succeedsOnFourthAttempt() {
        Task task = taskWith(Map.of("service", "payment-gateway"));

        // Exhaust first 3 failed attempts
        worker.execute(task);
        worker.execute(task);
        worker.execute(task);

        // Attempt 4 — should succeed
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("attempts"));
        assertEquals("healthy", result.getOutputData().get("serviceStatus"));
    }

    @Test
    void returnsAttemptsCount() {
        Task task = taskWith(Map.of("service", "test-service"));

        worker.execute(task);
        assertEquals(1, worker.getAttemptCount());

        worker.execute(task);
        assertEquals(2, worker.getAttemptCount());
    }

    @Test
    void resetAttemptCounter() {
        Task task = taskWith(Map.of("service", "test-service"));

        worker.execute(task);
        worker.execute(task);
        assertEquals(2, worker.getAttemptCount());

        worker.resetAttemptCounter();
        assertEquals(0, worker.getAttemptCount());

        // After reset, first attempt should fail again
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
    }

    @Test
    void defaultsServiceWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
    }

    @Test
    void defaultsServiceWhenBlank() {
        Task task = taskWith(Map.of("service", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
    }

    @Test
    void failedAttemptsIncludeReasonForIncompletion() {
        Task task = taskWith(Map.of("service", "api-gateway"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertNotNull(result.getReasonForIncompletion());
        assertTrue(result.getReasonForIncompletion().contains("Service unavailable"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
