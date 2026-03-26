package eventmonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeLatencyWorkerTest {

    private final AnalyzeLatencyWorker worker = new AnalyzeLatencyWorker();

    @Test
    void taskDefName() {
        assertEquals("em_analyze_latency", worker.getTaskDefName());
    }

    @Test
    void analyzesLatencyFromMetrics() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("avgLatencyMs", 45, "p95LatencyMs", 120, "p99LatencyMs", 500)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("latency"));
    }

    @Test
    void latencyContainsAvgMs() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("avgLatencyMs", 45, "p95LatencyMs", 120, "p99LatencyMs", 500)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> latency = (Map<String, Object>) result.getOutputData().get("latency");
        assertEquals(45, latency.get("avgMs"));
    }

    @Test
    void latencyContainsP95Ms() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("avgLatencyMs", 45, "p95LatencyMs", 120, "p99LatencyMs", 500)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> latency = (Map<String, Object>) result.getOutputData().get("latency");
        assertEquals(120, latency.get("p95Ms"));
    }

    @Test
    void latencyContainsP99Ms() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("avgLatencyMs", 45, "p95LatencyMs", 120, "p99LatencyMs", 500)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> latency = (Map<String, Object>) result.getOutputData().get("latency");
        assertEquals(500, latency.get("p99Ms"));
    }

    @Test
    void handlesNullMetrics() {
        Map<String, Object> input = new HashMap<>();
        input.put("metrics", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> latency = (Map<String, Object>) result.getOutputData().get("latency");
        assertEquals(0, latency.get("avgMs"));
        assertEquals(0, latency.get("p95Ms"));
        assertEquals(0, latency.get("p99Ms"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("latency"));
    }

    @Test
    void handlesPartialMetrics() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("avgLatencyMs", 50)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> latency = (Map<String, Object>) result.getOutputData().get("latency");
        assertEquals(50, latency.get("avgMs"));
        assertEquals(0, latency.get("p95Ms"));
        assertEquals(0, latency.get("p99Ms"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
