package eventcorrelation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InitCorrelationWorkerTest {

    private final InitCorrelationWorker worker = new InitCorrelationWorker();

    @Test
    void taskDefName() {
        assertEquals("ec_init_correlation", worker.getTaskDefName());
    }

    @Test
    void returnsCorrelationIdAndSessionStarted() {
        Task task = taskWith(Map.of("correlationId", "corr-fixed-001", "expectedEvents", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("corr-fixed-001", result.getOutputData().get("correlationId"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("sessionStarted"));
    }

    @Test
    void defaultsCorrelationIdWhenMissing() {
        Task task = taskWith(Map.of("expectedEvents", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("corr-unknown", result.getOutputData().get("correlationId"));
    }

    @Test
    void defaultsCorrelationIdWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("correlationId", null);
        input.put("expectedEvents", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("corr-unknown", result.getOutputData().get("correlationId"));
    }

    @Test
    void defaultsCorrelationIdWhenBlank() {
        Task task = taskWith(Map.of("correlationId", "   ", "expectedEvents", 3));
        TaskResult result = worker.execute(task);

        assertEquals("corr-unknown", result.getOutputData().get("correlationId"));
    }

    @Test
    void handlesCustomCorrelationId() {
        Task task = taskWith(Map.of("correlationId", "corr-custom-999", "expectedEvents", 5));
        TaskResult result = worker.execute(task);

        assertEquals("corr-custom-999", result.getOutputData().get("correlationId"));
    }

    @Test
    void sessionStartedIsFixedTimestamp() {
        Task task = taskWith(Map.of("correlationId", "corr-1", "expectedEvents", 1));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("sessionStarted"));
    }

    @Test
    void outputContainsExpectedFields() {
        Task task = taskWith(Map.of("correlationId", "corr-test", "expectedEvents", 3));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("correlationId"));
        assertTrue(result.getOutputData().containsKey("sessionStarted"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
