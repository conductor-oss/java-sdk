package eventmonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectMetricsWorkerTest {

    private final CollectMetricsWorker worker = new CollectMetricsWorker();

    @Test
    void taskDefName() {
        assertEquals("em_collect_metrics", worker.getTaskDefName());
    }

    @Test
    void collectsMetricsForPipeline() {
        Task task = taskWith(Map.of(
                "pipelineName", "order-events-pipeline",
                "timeRange", "last_1h"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("rawMetrics"));
    }

    @Test
    void rawMetricsContainsTotalEvents() {
        Task task = taskWith(Map.of(
                "pipelineName", "order-events-pipeline",
                "timeRange", "last_1h"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> rawMetrics = (Map<String, Object>) result.getOutputData().get("rawMetrics");
        assertEquals(15420, rawMetrics.get("totalEvents"));
    }

    @Test
    void rawMetricsContainsProcessedAndFailedEvents() {
        Task task = taskWith(Map.of(
                "pipelineName", "order-events-pipeline",
                "timeRange", "last_1h"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> rawMetrics = (Map<String, Object>) result.getOutputData().get("rawMetrics");
        assertEquals(15100, rawMetrics.get("processedEvents"));
        assertEquals(320, rawMetrics.get("failedEvents"));
    }

    @Test
    void rawMetricsContainsLatencyValues() {
        Task task = taskWith(Map.of(
                "pipelineName", "order-events-pipeline",
                "timeRange", "last_1h"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> rawMetrics = (Map<String, Object>) result.getOutputData().get("rawMetrics");
        assertEquals(45, rawMetrics.get("avgLatencyMs"));
        assertEquals(120, rawMetrics.get("p95LatencyMs"));
        assertEquals(500, rawMetrics.get("p99LatencyMs"));
    }

    @Test
    void handlesNullPipelineName() {
        Map<String, Object> input = new HashMap<>();
        input.put("pipelineName", null);
        input.put("timeRange", "last_1h");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("rawMetrics"));
    }

    @Test
    void handlesNullTimeRange() {
        Map<String, Object> input = new HashMap<>();
        input.put("pipelineName", "my-pipeline");
        input.put("timeRange", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("rawMetrics"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("rawMetrics"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
