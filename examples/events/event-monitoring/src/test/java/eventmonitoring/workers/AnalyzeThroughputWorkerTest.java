package eventmonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeThroughputWorkerTest {

    private final AnalyzeThroughputWorker worker = new AnalyzeThroughputWorker();

    @Test
    void taskDefName() {
        assertEquals("em_analyze_throughput", worker.getTaskDefName());
    }

    @Test
    void analyzesThroughputFromMetrics() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("totalEvents", 15420, "processedEvents", 15100)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("throughput"));
    }

    @Test
    void throughputContainsEventsPerSec() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("totalEvents", 15420)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> throughput = (Map<String, Object>) result.getOutputData().get("throughput");
        assertEquals("51.4", throughput.get("eventsPerSec"));
    }

    @Test
    void throughputContainsTotalEvents() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("totalEvents", 15420)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> throughput = (Map<String, Object>) result.getOutputData().get("throughput");
        assertEquals(15420, throughput.get("totalEvents"));
    }

    @Test
    void handlesZeroTotalEvents() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("totalEvents", 0)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> throughput = (Map<String, Object>) result.getOutputData().get("throughput");
        assertEquals(0, throughput.get("totalEvents"));
    }

    @Test
    void handlesNullMetrics() {
        Map<String, Object> input = new HashMap<>();
        input.put("metrics", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> throughput = (Map<String, Object>) result.getOutputData().get("throughput");
        assertEquals(0, throughput.get("totalEvents"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("throughput"));
    }

    @Test
    void handlesMissingTotalEventsKey() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("processedEvents", 15100)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> throughput = (Map<String, Object>) result.getOutputData().get("throughput");
        assertEquals(0, throughput.get("totalEvents"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
