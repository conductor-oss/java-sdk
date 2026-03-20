package custommetrics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DefineMetricsTest {

    private final DefineMetrics worker = new DefineMetrics();

    @Test void taskDefName() { assertEquals("cus_define_metrics", worker.getTaskDefName()); }

    @Test void returnsCompletedStatus() {
        TaskResult result = worker.execute(taskWith());
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void returnsRegisteredMetricsCount() {
        TaskResult result = worker.execute(taskWith());
        assertEquals(4, result.getOutputData().get("registeredMetrics"));
    }

    @Test void returnsMetricsList() {
        TaskResult result = worker.execute(taskWith());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) result.getOutputData().get("metrics");
        assertEquals(4, metrics.size());
    }

    @Test void firstMetricIsHistogram() {
        TaskResult result = worker.execute(taskWith());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) result.getOutputData().get("metrics");
        assertEquals("order_processing_time", metrics.get(0).get("name"));
        assertEquals("histogram", metrics.get(0).get("type"));
    }

    @Test void containsCounterType() {
        TaskResult result = worker.execute(taskWith());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) result.getOutputData().get("metrics");
        assertTrue(metrics.stream().anyMatch(m -> "counter".equals(m.get("type"))));
    }

    @Test void outputContainsAllExpectedKeys() {
        TaskResult result = worker.execute(taskWith());
        assertNotNull(result.getOutputData().get("registeredMetrics"));
        assertNotNull(result.getOutputData().get("metrics"));
    }

    private Task taskWith() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("metricDefinitions", List.of("a", "b"))));
        return task;
    }
}
