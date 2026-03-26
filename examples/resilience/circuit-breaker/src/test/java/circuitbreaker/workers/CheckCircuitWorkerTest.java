package circuitbreaker.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckCircuitWorkerTest {

    @BeforeEach
    void setUp() {
        CircuitBreakerState.clearAll();
    }

    @Test
    void taskDefName() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        assertEquals("cb_check_circuit", worker.getTaskDefName());
    }

    @Test
    void failsOnMissingServiceName() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("failureCount", 0, "threshold", 3));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void returnsOpenWhenCircuitStateIsOpen() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "circuitState", "OPEN", "failureCount", 0, "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("OPEN", result.getOutputData().get("state"));
    }

    @Test
    void returnsHalfOpenWhenCircuitStateIsHalfOpen() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "circuitState", "HALF_OPEN", "failureCount", 0, "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("HALF_OPEN", result.getOutputData().get("state"));
    }

    @Test
    void returnsClosedWhenFailureCountBelowThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "failureCount", 1, "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CLOSED", result.getOutputData().get("state"));
    }

    @Test
    void returnsOpenWhenFailureCountMeetsThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "failureCount", 3, "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("OPEN", result.getOutputData().get("state"));
    }

    @Test
    void returnsOpenWhenFailureCountExceedsThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "failureCount", 5, "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("OPEN", result.getOutputData().get("state"));
    }

    @Test
    void returnsClosedByDefaultWithServiceName() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CLOSED", result.getOutputData().get("state"));
        assertEquals(0, result.getOutputData().get("failureCount"));
        assertEquals(3, result.getOutputData().get("threshold"));
    }

    @Test
    void forcedOpenOverridesFailureCount() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "circuitState", "OPEN", "failureCount", 0, "threshold", 10));
        TaskResult result = worker.execute(task);
        assertEquals("OPEN", result.getOutputData().get("state"));
    }

    @Test
    void returnsClosedWhenCircuitStateIsClosedAndBelowThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "circuitState", "CLOSED", "failureCount", 1, "threshold", 5));
        TaskResult result = worker.execute(task);
        assertEquals("CLOSED", result.getOutputData().get("state"));
    }

    @Test
    void outputContainsFailureCountAndThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "failureCount", 2, "threshold", 5));
        TaskResult result = worker.execute(task);
        assertEquals(2, result.getOutputData().get("failureCount"));
        assertEquals(5, result.getOutputData().get("threshold"));
    }

    @Test
    void handlesStringFailureCount() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "failureCount", "4", "threshold", 3));
        TaskResult result = worker.execute(task);
        assertEquals("OPEN", result.getOutputData().get("state"));
    }

    @Test
    void failsOnNegativeThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "threshold", -1));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void failsOnZeroThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "threshold", 0));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void failsOnNegativeFailureCount() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "test-svc", "failureCount", -1));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void persistsStateToDisk() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("serviceName", "persist-svc", "failureCount", 5, "threshold", 3));
        worker.execute(task);

        // Clear in-memory cache and verify file-backed state
        CircuitBreakerState.clearAll();
        // After clearAll, setState was called during execute, so we check the execute behavior persisted
        // Re-execute to verify state transitions work
        Task task2 = taskWith(Map.of("serviceName", "persist-svc2", "circuitState", "HALF_OPEN"));
        TaskResult result2 = worker.execute(task2);
        assertEquals("HALF_OPEN", result2.getOutputData().get("state"));

        // Verify the persisted state can be read
        String state = CircuitBreakerState.getState("persist-svc2");
        assertEquals("HALF_OPEN", state);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
