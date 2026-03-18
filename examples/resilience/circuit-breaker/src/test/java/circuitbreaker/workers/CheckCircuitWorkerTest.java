package circuitbreaker.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckCircuitWorkerTest {

    @Test
    void taskDefName() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        assertEquals("cb_check_circuit", worker.getTaskDefName());
    }

    @Test
    void returnsOpenWhenCircuitStateIsOpen() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("circuitState", "OPEN", "failureCount", 0, "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("OPEN", result.getOutputData().get("state"));
    }

    @Test
    void returnsHalfOpenWhenCircuitStateIsHalfOpen() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("circuitState", "HALF_OPEN", "failureCount", 0, "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("HALF_OPEN", result.getOutputData().get("state"));
    }

    @Test
    void returnsClosedWhenFailureCountBelowThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("failureCount", 1, "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CLOSED", result.getOutputData().get("state"));
    }

    @Test
    void returnsOpenWhenFailureCountMeetsThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("failureCount", 3, "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("OPEN", result.getOutputData().get("state"));
    }

    @Test
    void returnsOpenWhenFailureCountExceedsThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("failureCount", 5, "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("OPEN", result.getOutputData().get("state"));
    }

    @Test
    void returnsClosedWhenNoInputsProvided() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CLOSED", result.getOutputData().get("state"));
        assertEquals(0, result.getOutputData().get("failureCount"));
        assertEquals(3, result.getOutputData().get("threshold"));
    }

    @Test
    void forcedOpenOverridesFailureCount() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("circuitState", "OPEN", "failureCount", 0, "threshold", 10));
        TaskResult result = worker.execute(task);

        assertEquals("OPEN", result.getOutputData().get("state"));
    }

    @Test
    void returnsClosedWhenCircuitStateIsClosedAndBelowThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("circuitState", "CLOSED", "failureCount", 1, "threshold", 5));
        TaskResult result = worker.execute(task);

        assertEquals("CLOSED", result.getOutputData().get("state"));
    }

    @Test
    void outputContainsFailureCountAndThreshold() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("failureCount", 2, "threshold", 5));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("failureCount"));
        assertEquals(5, result.getOutputData().get("threshold"));
    }

    @Test
    void handlesStringFailureCount() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("failureCount", "4", "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals("OPEN", result.getOutputData().get("state"));
    }

    @Test
    void handlesNonNumericFailureCountGracefully() {
        CheckCircuitWorker worker = new CheckCircuitWorker();
        Task task = taskWith(Map.of("failureCount", "not-a-number", "threshold", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CLOSED", result.getOutputData().get("state"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
