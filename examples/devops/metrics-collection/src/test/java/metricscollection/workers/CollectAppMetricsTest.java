package metricscollection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectAppMetricsTest {

    private final CollectAppMetrics worker = new CollectAppMetrics();

    @Test
    void taskDefName() {
        assertEquals("mc_collect_app", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("environment", "production", "source", "application"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsMetricCount() {
        Task task = taskWith(Map.of("environment", "production", "source", "application"));
        TaskResult result = worker.execute(task);
        assertEquals(45, result.getOutputData().get("metricCount"));
    }

    @Test
    void returnsSource() {
        Task task = taskWith(Map.of("environment", "staging", "source", "application"));
        TaskResult result = worker.execute(task);
        assertEquals("application", result.getOutputData().get("source"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsMetricsMap() {
        Task task = taskWith(Map.of("environment", "production", "source", "application"));
        TaskResult result = worker.execute(task);
        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("metrics");
        assertNotNull(metrics);
        assertEquals(1200, metrics.get("requestRate"));
        assertEquals(0.5, metrics.get("errorRate"));
        assertEquals(230, metrics.get("p99Latency"));
    }

    @Test
    void handlesNullEnvironment() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(45, result.getOutputData().get("metricCount"));
    }

    @Test
    void defaultsSourceWhenMissing() {
        Task task = taskWith(Map.of("environment", "production"));
        TaskResult result = worker.execute(task);
        assertEquals("application", result.getOutputData().get("source"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
