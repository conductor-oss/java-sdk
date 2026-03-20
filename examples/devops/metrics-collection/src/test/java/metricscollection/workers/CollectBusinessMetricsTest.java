package metricscollection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectBusinessMetricsTest {

    private final CollectBusinessMetrics worker = new CollectBusinessMetrics();

    @Test
    void taskDefName() {
        assertEquals("mc_collect_business", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("environment", "production", "source", "business"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsMetricCount() {
        Task task = taskWith(Map.of("environment", "production", "source", "business"));
        TaskResult result = worker.execute(task);
        assertEquals(18, result.getOutputData().get("metricCount"));
    }

    @Test
    void returnsSource() {
        Task task = taskWith(Map.of("environment", "staging", "source", "business"));
        TaskResult result = worker.execute(task);
        assertEquals("business", result.getOutputData().get("source"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsMetricsMap() {
        Task task = taskWith(Map.of("environment", "production", "source", "business"));
        TaskResult result = worker.execute(task);
        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("metrics");
        assertNotNull(metrics);
        assertEquals(24500, metrics.get("revenue"));
        assertEquals(340, metrics.get("orders"));
        assertEquals(3.2, metrics.get("conversionRate"));
    }

    @Test
    void handlesNullEnvironment() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(18, result.getOutputData().get("metricCount"));
    }

    @Test
    void defaultsSourceWhenMissing() {
        Task task = taskWith(Map.of("environment", "production"));
        TaskResult result = worker.execute(task);
        assertEquals("business", result.getOutputData().get("source"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
