package eventaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PublishBatchWorkerTest {

    private final PublishBatchWorker worker = new PublishBatchWorker();

    @Test
    void taskDefName() {
        assertEquals("eg_publish_batch", worker.getTaskDefName());
    }

    @Test
    void publishesSuccessfully() {
        Task task = taskWith(Map.of(
                "summary", sampleSummary(),
                "destination", "analytics_pipeline"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
    }

    @Test
    void returnsBatchId() {
        Task task = taskWith(Map.of(
                "summary", sampleSummary(),
                "destination", "analytics_pipeline"));
        TaskResult result = worker.execute(task);

        assertEquals("batch-fixed-001", result.getOutputData().get("batchId"));
    }

    @Test
    void returnsPublishedAt() {
        Task task = taskWith(Map.of(
                "summary", sampleSummary(),
                "destination", "analytics_pipeline"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("publishedAt"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("batch-fixed-001", result.getOutputData().get("batchId"));
        assertEquals(true, result.getOutputData().get("published"));
    }

    @Test
    void handlesNullSummary() {
        Map<String, Object> input = new HashMap<>();
        input.put("summary", null);
        input.put("destination", "analytics_pipeline");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("batch-fixed-001", result.getOutputData().get("batchId"));
    }

    @Test
    void handlesNullDestination() {
        Map<String, Object> input = new HashMap<>();
        input.put("summary", sampleSummary());
        input.put("destination", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
    }

    @Test
    void returnsAllOutputFields() {
        Task task = taskWith(Map.of(
                "summary", sampleSummary(),
                "destination", "analytics_pipeline"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("batchId"));
        assertNotNull(result.getOutputData().get("published"));
        assertNotNull(result.getOutputData().get("publishedAt"));
        assertEquals(3, result.getOutputData().size());
    }

    @Test
    void handlesEmptyDestination() {
        Map<String, Object> input = new HashMap<>();
        input.put("summary", sampleSummary());
        input.put("destination", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("batch-fixed-001", result.getOutputData().get("batchId"));
    }

    private Map<String, Object> sampleSummary() {
        return Map.of(
                "windowId", "win-fixed-001",
                "generatedAt", "2026-01-15T10:00:00Z",
                "metrics", Map.of("totalRevenue", 478.47),
                "highlights", List.of("6 events processed")
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
