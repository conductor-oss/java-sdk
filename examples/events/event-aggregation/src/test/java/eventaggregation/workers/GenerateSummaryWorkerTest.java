package eventaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateSummaryWorkerTest {

    private final GenerateSummaryWorker worker = new GenerateSummaryWorker();

    @Test
    void taskDefName() {
        assertEquals("eg_generate_summary", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatesSummaryWithWindowId() {
        Task task = taskWith(Map.of(
                "aggregation", sampleAggregation(),
                "windowId", "win-fixed-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertEquals("win-fixed-001", summary.get("windowId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsGeneratedAtTimestamp() {
        Task task = taskWith(Map.of(
                "aggregation", sampleAggregation(),
                "windowId", "win-fixed-001"));
        TaskResult result = worker.execute(task);

        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");
        assertEquals("2026-01-15T10:00:00Z", summary.get("generatedAt"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsHighlights() {
        Task task = taskWith(Map.of(
                "aggregation", sampleAggregation(),
                "windowId", "win-fixed-001"));
        TaskResult result = worker.execute(task);

        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");
        List<String> highlights = (List<String>) summary.get("highlights");
        assertNotNull(highlights);
        assertEquals(3, highlights.size());
        assertEquals("6 events processed", highlights.get(0));
        assertEquals("Net revenue: $478.47", highlights.get(1));
        assertEquals("Top product: Widget B (2 events)", highlights.get(2));
    }

    @Test
    void returnsDestination() {
        Task task = taskWith(Map.of(
                "aggregation", sampleAggregation(),
                "windowId", "win-fixed-001"));
        TaskResult result = worker.execute(task);

        assertEquals("analytics_pipeline", result.getOutputData().get("destination"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void includesMetricsInSummary() {
        Task task = taskWith(Map.of(
                "aggregation", sampleAggregation(),
                "windowId", "win-fixed-001"));
        TaskResult result = worker.execute(task);

        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");
        Map<String, Object> metrics = (Map<String, Object>) summary.get("metrics");
        assertNotNull(metrics);
        assertEquals(478.47, metrics.get("totalRevenue"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
        assertEquals("analytics_pipeline", result.getOutputData().get("destination"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesNullAggregation() {
        Map<String, Object> input = new HashMap<>();
        input.put("aggregation", null);
        input.put("windowId", "win-test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertEquals("win-test", summary.get("windowId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesNullWindowId() {
        Map<String, Object> input = new HashMap<>();
        input.put("aggregation", sampleAggregation());
        input.put("windowId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> summary =
                (Map<String, Object>) result.getOutputData().get("summary");
        assertEquals("win-default", summary.get("windowId"));
    }

    private Map<String, Object> sampleAggregation() {
        return Map.of(
                "totalEvents", 6,
                "purchaseCount", 5,
                "refundCount", 1,
                "totalRevenue", 478.47,
                "avgOrderValue", 100.70,
                "productBreakdown", Map.of(
                        "Widget A", 3,
                        "Widget B", 2,
                        "Widget C", 1
                )
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
