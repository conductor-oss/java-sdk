package retryexponential.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetryExpoTaskWorkerTest {

    private RetryExpoTaskWorker worker;

    @BeforeEach
    void setUp() {
        worker = new RetryExpoTaskWorker(2);
    }

    @Test
    void taskDefName() {
        assertEquals("retry_expo_task", worker.getTaskDefName());
    }

    @Test
    void firstAttemptFails() {
        Task task = taskWith(Map.of("apiUrl", "https://api.example.com/data"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
        assertEquals("429 Too Many Requests", result.getOutputData().get("error"));
        assertNotNull(result.getReasonForIncompletion());
        assertTrue(result.getReasonForIncompletion().contains("429"));
    }

    @Test
    void secondAttemptFails() {
        Task task1 = taskWith(Map.of("apiUrl", "https://api.example.com/data"));
        worker.execute(task1);

        Task task2 = taskWith(Map.of("apiUrl", "https://api.example.com/data"));
        TaskResult result = worker.execute(task2);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(2, result.getOutputData().get("attempts"));
    }

    @Test
    void thirdAttemptSucceeds() {
        Task task1 = taskWith(Map.of("apiUrl", "https://api.example.com/data"));
        worker.execute(task1);
        Task task2 = taskWith(Map.of("apiUrl", "https://api.example.com/data"));
        worker.execute(task2);

        Task task3 = taskWith(Map.of("apiUrl", "https://api.example.com/data"));
        TaskResult result = worker.execute(task3);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("attempts"));
        assertNotNull(result.getOutputData().get("data"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("data");
        assertEquals("ok", data.get("status"));
    }

    @Test
    void outputContainsAttemptsKey() {
        Task task = taskWith(Map.of("apiUrl", "https://api.example.com/data"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("attempts"));
        assertNotNull(result.getOutputData().get("attempts"));
    }

    @Test
    void defaultsApiUrlWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        // Should still execute (and fail on attempt 1) without error
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
    }

    @Test
    void defaultsApiUrlWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("apiUrl", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        // Fresh worker from @BeforeEach, so this is attempt 1
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
    }

    @Test
    void defaultsApiUrlWhenBlank() {
        // Exhaust the 2 failures first, then blank apiUrl should still succeed
        worker.execute(taskWith(Map.of("apiUrl", "x")));
        worker.execute(taskWith(Map.of("apiUrl", "x")));

        Task task = taskWith(Map.of("apiUrl", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void resetAttemptsWorks() {
        Task task = taskWith(Map.of("apiUrl", "https://api.example.com/data"));
        worker.execute(task);
        assertEquals(1, worker.getAttemptCount());

        worker.resetAttempts();
        assertEquals(0, worker.getAttemptCount());

        // After reset, first attempt should fail again
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
    }

    @Test
    void workerWithZeroFailures() {
        RetryExpoTaskWorker alwaysSucceed = new RetryExpoTaskWorker(0);
        Task task = taskWith(Map.of("apiUrl", "https://api.example.com/data"));
        TaskResult result = alwaysSucceed.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempts"));
    }

    @Test
    void failedAttemptContainsErrorField() {
        worker.resetAttempts();
        Task task = taskWith(Map.of("apiUrl", "https://api.example.com/data"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("error"));
        assertEquals("429 Too Many Requests", result.getOutputData().get("error"));
    }

    @Test
    void successfulAttemptContainsDataField() {
        worker.resetAttempts();
        // Exhaust failures
        worker.execute(taskWith(Map.of("apiUrl", "x")));
        worker.execute(taskWith(Map.of("apiUrl", "x")));

        // This should succeed
        TaskResult result = worker.execute(taskWith(Map.of("apiUrl", "x")));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("data"));
        assertFalse(result.getOutputData().containsKey("error"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
