package eventmonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeErrorsWorkerTest {

    private final AnalyzeErrorsWorker worker = new AnalyzeErrorsWorker();

    @Test
    void taskDefName() {
        assertEquals("em_analyze_errors", worker.getTaskDefName());
    }

    @Test
    void analyzesErrorRateFromMetrics() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("failedEvents", 320, "totalEvents", 15420)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("errorRate"));
    }

    @Test
    void errorRateContainsPercentage() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("failedEvents", 320, "totalEvents", 15420)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> errorRate = (Map<String, Object>) result.getOutputData().get("errorRate");
        assertEquals("2.08", errorRate.get("percentage"));
    }

    @Test
    void errorRateContainsFailedAndTotal() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("failedEvents", 320, "totalEvents", 15420)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> errorRate = (Map<String, Object>) result.getOutputData().get("errorRate");
        assertEquals(320, errorRate.get("failed"));
        assertEquals(15420, errorRate.get("total"));
    }

    @Test
    void handlesZeroTotalEvents() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("failedEvents", 0, "totalEvents", 0)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> errorRate = (Map<String, Object>) result.getOutputData().get("errorRate");
        assertEquals("0.00", errorRate.get("percentage"));
    }

    @Test
    void handlesNullMetrics() {
        Map<String, Object> input = new HashMap<>();
        input.put("metrics", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> errorRate = (Map<String, Object>) result.getOutputData().get("errorRate");
        assertEquals("0.00", errorRate.get("percentage"));
        assertEquals(0, errorRate.get("failed"));
        assertEquals(0, errorRate.get("total"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("errorRate"));
    }

    @Test
    void handlesNoFailedEvents() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("failedEvents", 0, "totalEvents", 10000)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> errorRate = (Map<String, Object>) result.getOutputData().get("errorRate");
        assertEquals("0.00", errorRate.get("percentage"));
        assertEquals(0, errorRate.get("failed"));
        assertEquals(10000, errorRate.get("total"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
