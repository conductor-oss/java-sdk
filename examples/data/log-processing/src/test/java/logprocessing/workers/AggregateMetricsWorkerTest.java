package logprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateMetricsWorkerTest {

    private final AggregateMetricsWorker worker = new AggregateMetricsWorker();

    @Test
    void taskDefName() {
        assertEquals("lp_aggregate_metrics", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void aggregatesMetricsByService() {
        List<Map<String, Object>> entries = List.of(
                Map.of("service", "api-gateway", "isError", false),
                Map.of("service", "api-gateway", "isError", false),
                Map.of("service", "auth-service", "isError", true));
        Task task = taskWith(Map.of("entries", entries));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("metrics");
        assertNotNull(metrics);
        assertEquals(3, metrics.get("totalEntries"));

        Map<String, Map<String, Object>> byService = (Map<String, Map<String, Object>>) metrics.get("byService");
        assertEquals(2, byService.get("api-gateway").get("total"));
        assertEquals(0, byService.get("api-gateway").get("errors"));
        assertEquals(1, byService.get("auth-service").get("total"));
        assertEquals(1, byService.get("auth-service").get("errors"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void calculatesErrorRate() {
        List<Map<String, Object>> entries = List.of(
                Map.of("service", "svc", "isError", true),
                Map.of("service", "svc", "isError", false));
        Task task = taskWith(Map.of("entries", entries));
        TaskResult result = worker.execute(task);

        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("metrics");
        assertEquals("50.0%", metrics.get("errorRate"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesEmptyEntries() {
        Task task = taskWith(Map.of("entries", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("metrics");
        assertEquals(0, metrics.get("totalEntries"));
        assertEquals("0.0%", metrics.get("errorRate"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
