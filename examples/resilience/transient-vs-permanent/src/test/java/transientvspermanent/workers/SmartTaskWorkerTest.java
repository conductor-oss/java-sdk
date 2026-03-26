package transientvspermanent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmartTaskWorkerTest {

    @Test
    void taskDefName() {
        SmartTaskWorker worker = new SmartTaskWorker();
        assertEquals("tvp_smart_task", worker.getTaskDefName());
    }

    // --- No error type (default): immediate success ---

    @Test
    void completesImmediatelyWhenNoErrorType() {
        SmartTaskWorker worker = new SmartTaskWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempt"));
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void completesImmediatelyWhenErrorTypeIsNull() {
        SmartTaskWorker worker = new SmartTaskWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("errorType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempt"));
        assertEquals("success", result.getOutputData().get("result"));
    }

    // --- Transient error: fail first 2, succeed on 3rd ---

    @Test
    void transientErrorFailsOnFirstAttempt() {
        SmartTaskWorker worker = new SmartTaskWorker();
        Task task = taskWith(Map.of("errorType", "transient"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempt"));
        assertEquals("transient", result.getOutputData().get("errorType"));
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void transientErrorFailsOnSecondAttempt() {
        SmartTaskWorker worker = new SmartTaskWorker();

        // First attempt
        Task task1 = taskWith(Map.of("errorType", "transient"));
        worker.execute(task1);

        // Second attempt
        Task task2 = taskWith(Map.of("errorType", "transient"));
        TaskResult result = worker.execute(task2);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(2, result.getOutputData().get("attempt"));
        assertEquals("transient", result.getOutputData().get("errorType"));
    }

    @Test
    void transientErrorSucceedsOnThirdAttempt() {
        SmartTaskWorker worker = new SmartTaskWorker();

        // First two attempts fail
        worker.execute(taskWith(Map.of("errorType", "transient")));
        worker.execute(taskWith(Map.of("errorType", "transient")));

        // Third attempt succeeds
        Task task3 = taskWith(Map.of("errorType", "transient"));
        TaskResult result = worker.execute(task3);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("attempt"));
        assertEquals("transient", result.getOutputData().get("errorType"));
        assertEquals("success after transient errors", result.getOutputData().get("result"));
    }

    // --- Permanent error: immediate terminal failure ---

    @Test
    void permanentErrorFailsWithTerminalError() {
        SmartTaskWorker worker = new SmartTaskWorker();
        Task task = taskWith(Map.of("errorType", "permanent"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempt"));
        assertEquals("permanent", result.getOutputData().get("errorType"));
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void permanentErrorOutputContainsNoRetryMessage() {
        SmartTaskWorker worker = new SmartTaskWorker();
        Task task = taskWith(Map.of("errorType", "permanent"));
        TaskResult result = worker.execute(task);

        String error = (String) result.getOutputData().get("error");
        assertTrue(error.contains("no retries") || error.contains("Permanent"),
                "Error message should indicate no retries");
    }

    // --- Reset method ---

    @Test
    void resetClearsAttemptCounter() {
        SmartTaskWorker worker = new SmartTaskWorker();

        // Increment counter
        worker.execute(taskWith(Map.of("errorType", "transient")));
        worker.execute(taskWith(Map.of("errorType", "transient")));

        // Reset
        worker.reset();

        // After reset, attempt should be 1 again
        Task task = taskWith(Map.of("errorType", "transient"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempt"));
    }

    // --- Edge cases ---

    @Test
    void handlesUnknownErrorTypeAsDefault() {
        SmartTaskWorker worker = new SmartTaskWorker();
        Task task = taskWith(Map.of("errorType", "unknown"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempt"));
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void handlesNonStringErrorTypeGracefully() {
        SmartTaskWorker worker = new SmartTaskWorker();
        Task task = taskWith(Map.of("errorType", 42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("attempt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
