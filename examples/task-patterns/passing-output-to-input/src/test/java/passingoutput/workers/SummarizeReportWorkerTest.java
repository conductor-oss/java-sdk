package passingoutput.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SummarizeReportWorkerTest {

    private final SummarizeReportWorker worker = new SummarizeReportWorker();

    @Test
    void taskDefName() {
        assertEquals("summarize_report", worker.getTaskDefName());
    }

    @Test
    void generatesSummaryString() {
        Task task = taskWith(Map.of(
                "originalMetrics", Map.of("revenue", 125000, "orders", 3400),
                "enrichedInsights", Map.of("healthScore", "HEALTHY", "yoyGrowth", "13.6%", "topProduct", "Widget A"),
                "growthRate", 13.6,
                "region", "US-West",
                "period", "Q1-2026"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertEquals("US-West Q1-2026: $125000 revenue, 13.6% growth, HEALTHY", summary);
    }

    @Test
    void recommendsExpandForHighGrowth() {
        Task task = taskWith(Map.of(
                "originalMetrics", Map.of("revenue", 125000),
                "enrichedInsights", Map.of("healthScore", "HEALTHY"),
                "growthRate", 15.0,
                "region", "US-West",
                "period", "Q1-2026"
        ));
        TaskResult result = worker.execute(task);

        assertEquals("Expand operations — strong growth trajectory",
                result.getOutputData().get("recommendation"));
    }

    @Test
    void recommendsMaintainForModerateGrowth() {
        Task task = taskWith(Map.of(
                "originalMetrics", Map.of("revenue", 125000),
                "enrichedInsights", Map.of("healthScore", "HEALTHY"),
                "growthRate", 5.0,
                "region", "US-West",
                "period", "Q1-2026"
        ));
        TaskResult result = worker.execute(task);

        assertEquals("Maintain course — steady growth",
                result.getOutputData().get("recommendation"));
    }

    @Test
    void recommendsReviewForNegativeGrowth() {
        Task task = taskWith(Map.of(
                "originalMetrics", Map.of("revenue", 100000),
                "enrichedInsights", Map.of("healthScore", "NEEDS ATTENTION"),
                "growthRate", -3.0,
                "region", "US-East",
                "period", "Q2-2026"
        ));
        TaskResult result = worker.execute(task);

        assertEquals("Review strategy — declining metrics",
                result.getOutputData().get("recommendation"));
    }

    @Test
    void outputContainsSummaryAndRecommendation() {
        Task task = taskWith(Map.of(
                "originalMetrics", Map.of("revenue", 125000),
                "enrichedInsights", Map.of("healthScore", "HEALTHY"),
                "growthRate", 13.6,
                "region", "US-West",
                "period", "Q1-2026"
        ));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("summary"));
        assertTrue(result.getOutputData().containsKey("recommendation"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
