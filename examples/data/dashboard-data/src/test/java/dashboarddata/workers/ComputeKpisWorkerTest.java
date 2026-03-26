package dashboarddata.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComputeKpisWorkerTest {

    private final ComputeKpisWorker worker = new ComputeKpisWorker();

    @Test
    void taskDefName() {
        assertEquals("dh_compute_kpis", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void computesKpis() {
        Map<String, Object> metrics = Map.of(
                "activeUsers", Map.of("current", 1247, "previous", 1180),
                "revenue", Map.of("current", 89500, "previous", 82000),
                "errorRate", Map.of("current", 0.8),
                "avgResponseTime", Map.of("current", 182),
                "conversionRate", Map.of("current", 3.2));
        Task task = taskWith(Map.of("metrics", metrics));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> kpis = (List<Map<String, Object>>) result.getOutputData().get("kpis");
        assertNotNull(kpis);
        assertEquals(5, kpis.size());
    }

    @Test
    void returnsKpiCount() {
        Map<String, Object> metrics = Map.of(
                "activeUsers", Map.of("current", 100, "previous", 90),
                "revenue", Map.of("current", 1000, "previous", 900),
                "errorRate", Map.of("current", 0.5),
                "avgResponseTime", Map.of("current", 100),
                "conversionRate", Map.of("current", 5.0));
        Task task = taskWith(Map.of("metrics", metrics));
        TaskResult result = worker.execute(task);
        assertEquals(5, result.getOutputData().get("kpiCount"));
    }

    @Test
    void handlesEmptyMetrics() {
        Task task = taskWith(Map.of("metrics", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
