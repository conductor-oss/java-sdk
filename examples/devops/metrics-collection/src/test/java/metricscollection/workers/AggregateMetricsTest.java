package metricscollection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateMetricsTest {

    private final AggregateMetrics worker = new AggregateMetrics();

    @Test
    void taskDefName() {
        assertEquals("mc_aggregate", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("appMetrics", 45, "infraMetrics", 32, "bizMetrics", 18));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void sumsAllMetricCounts() {
        Task task = taskWith(Map.of("appMetrics", 45, "infraMetrics", 32, "bizMetrics", 18));
        TaskResult result = worker.execute(task);
        assertEquals(95, result.getOutputData().get("totalMetrics"));
    }

    @Test
    void returnsSourceCount() {
        Task task = taskWith(Map.of("appMetrics", 10, "infraMetrics", 20, "bizMetrics", 30));
        TaskResult result = worker.execute(task);
        assertEquals(3, result.getOutputData().get("sources"));
    }

    @Test
    void returnsAggregatedTimestamp() {
        Task task = taskWith(Map.of("appMetrics", 1, "infraMetrics", 2, "bizMetrics", 3));
        TaskResult result = worker.execute(task);
        assertEquals("2026-03-08T06:00:00Z", result.getOutputData().get("aggregatedAt"));
    }

    @Test
    void handlesStringInputs() {
        Task task = taskWith(Map.of("appMetrics", "45", "infraMetrics", "32", "bizMetrics", "18"));
        TaskResult result = worker.execute(task);
        assertEquals(95, result.getOutputData().get("totalMetrics"));
    }

    @Test
    void handlesNullInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalMetrics"));
    }

    @Test
    void handlesZeroMetrics() {
        Task task = taskWith(Map.of("appMetrics", 0, "infraMetrics", 0, "bizMetrics", 0));
        TaskResult result = worker.execute(task);
        assertEquals(0, result.getOutputData().get("totalMetrics"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
