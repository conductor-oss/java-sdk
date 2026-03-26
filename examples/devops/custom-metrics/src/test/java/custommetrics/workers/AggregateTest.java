package custommetrics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AggregateTest {

    private final Aggregate worker = new Aggregate();

    @Test void taskDefName() { assertEquals("cus_aggregate", worker.getTaskDefName()); }

    @Test void returnsCompletedStatus() {
        TaskResult result = worker.execute(taskWith(4800, "5m"));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void returnsMetricCount() {
        TaskResult result = worker.execute(taskWith(4800, "5m"));
        assertEquals(4, result.getOutputData().get("metricCount"));
    }

    @Test void returnsAggregatedMetrics() {
        TaskResult result = worker.execute(taskWith(4800, "5m"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> agg = (List<Map<String, Object>>) result.getOutputData().get("aggregatedMetrics");
        assertEquals(4, agg.size());
    }

    @Test void firstMetricHasPercentiles() {
        TaskResult result = worker.execute(taskWith(4800, "5m"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> agg = (List<Map<String, Object>>) result.getOutputData().get("aggregatedMetrics");
        assertEquals("order_processing_time", agg.get(0).get("name"));
        assertEquals(1200, agg.get(0).get("p50"));
        assertEquals(5000, agg.get(0).get("p99"));
    }

    @Test void containsCheckoutSuccessCount() {
        TaskResult result = worker.execute(taskWith(4800, "5m"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> agg = (List<Map<String, Object>>) result.getOutputData().get("aggregatedMetrics");
        assertTrue(agg.stream().anyMatch(m -> "checkout_success_count".equals(m.get("name"))));
    }

    @Test void handlesNullWindow() {
        TaskResult result = worker.execute(taskWith(4800, null));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(int rawDataPoints, String window) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("rawDataPoints", rawDataPoints);
        if (window != null) input.put("aggregationWindow", window);
        task.setInputData(input);
        return task;
    }
}
