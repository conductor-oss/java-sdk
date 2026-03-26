package dashboarddata.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateMetricsWorkerTest {

    private final AggregateMetricsWorker worker = new AggregateMetricsWorker();

    @Test
    void taskDefName() {
        assertEquals("dh_aggregate_metrics", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsMetrics() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("metrics");
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("activeUsers"));
        assertTrue(metrics.containsKey("revenue"));
        assertTrue(metrics.containsKey("errorRate"));
    }

    @Test
    void returnsMetricCount() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(8, result.getOutputData().get("metricCount"));
    }

    @Test
    void returnsConsistentResults() {
        Task task = taskWith(Map.of());
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData().get("metricCount"), r2.getOutputData().get("metricCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
