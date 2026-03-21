package apmworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DetectBottlenecksTest {

    private final DetectBottlenecks worker = new DetectBottlenecks();

    @Test
    void taskDefName() {
        assertEquals("apm_detect_bottlenecks", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(520, 45);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsBottleneckCount() {
        Task task = taskWith(520, 45);
        TaskResult result = worker.execute(task);
        assertEquals(2, result.getOutputData().get("bottleneckCount"));
    }

    @Test
    void returnsBottlenecksList() {
        Task task = taskWith(520, 45);
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bottlenecks = (List<Map<String, Object>>) result.getOutputData().get("bottlenecks");
        assertEquals(2, bottlenecks.size());
        assertEquals("/api/search", bottlenecks.get(0).get("endpoint"));
        assertEquals("high", bottlenecks.get(0).get("impact"));
    }

    @Test
    void returnsRecommendations() {
        Task task = taskWith(520, 45);
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<String> recs = (List<String>) result.getOutputData().get("recommendations");
        assertEquals(2, recs.size());
        assertTrue(recs.get(0).contains("database index"));
    }

    @Test
    void secondBottleneckHasMediumImpact() {
        Task task = taskWith(520, 45);
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bottlenecks = (List<Map<String, Object>>) result.getOutputData().get("bottlenecks");
        assertEquals("medium", bottlenecks.get(1).get("impact"));
        assertEquals("/api/export", bottlenecks.get(1).get("endpoint"));
    }

    @Test
    void handlesNullInputGracefully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(520, 45);
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("bottleneckCount"));
        assertNotNull(result.getOutputData().get("bottlenecks"));
        assertNotNull(result.getOutputData().get("recommendations"));
    }

    private Task taskWith(int p99Latency, int p50Latency) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("p99Latency", p99Latency);
        input.put("p50Latency", p50Latency);
        input.put("slowEndpoints", List.of("/api/search", "/api/export"));
        task.setInputData(input);
        return task;
    }
}
