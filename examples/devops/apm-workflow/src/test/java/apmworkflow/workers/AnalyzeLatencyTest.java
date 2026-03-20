package apmworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeLatencyTest {

    private final AnalyzeLatency worker = new AnalyzeLatency();

    @Test
    void taskDefName() {
        assertEquals("apm_analyze_latency", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(25000, "p99");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsP50Latency() {
        Task task = taskWith(25000, "p99");
        TaskResult result = worker.execute(task);
        assertEquals(45, result.getOutputData().get("p50Latency"));
    }

    @Test
    void returnsP99Latency() {
        Task task = taskWith(25000, "p99");
        TaskResult result = worker.execute(task);
        assertEquals(520, result.getOutputData().get("p99Latency"));
    }

    @Test
    void returnsMeanLatency() {
        Task task = taskWith(25000, "p99");
        TaskResult result = worker.execute(task);
        assertEquals(62, result.getOutputData().get("meanLatency"));
    }

    @Test
    void returnsSlowEndpoints() {
        Task task = taskWith(25000, "p99");
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<String> slow = (List<String>) result.getOutputData().get("slowEndpoints");
        assertEquals(2, slow.size());
        assertTrue(slow.contains("/api/search"));
        assertTrue(slow.contains("/api/export"));
    }

    @Test
    void returnsP95Latency() {
        Task task = taskWith(25000, "p95");
        TaskResult result = worker.execute(task);
        assertEquals(180, result.getOutputData().get("p95Latency"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(10000, "p99");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("p50Latency"));
        assertNotNull(result.getOutputData().get("p95Latency"));
        assertNotNull(result.getOutputData().get("p99Latency"));
        assertNotNull(result.getOutputData().get("meanLatency"));
        assertNotNull(result.getOutputData().get("slowEndpoints"));
    }

    private Task taskWith(int traceCount, String percentile) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("traceCount", traceCount);
        input.put("percentile", percentile);
        task.setInputData(input);
        return task;
    }
}
