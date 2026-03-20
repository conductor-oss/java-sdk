package passingoutput.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnrichReportWorkerTest {

    private final EnrichReportWorker worker = new EnrichReportWorker();

    @Test
    void taskDefName() {
        assertEquals("enrich_report", worker.getTaskDefName());
    }

    @Test
    void computesGrowthRate() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("revenue", 125000, "orders", 3400, "avgOrderValue", 36.76, "returnRate", 0.03),
                "topProducts", List.of(
                        Map.of("name", "Widget A", "sales", 1200),
                        Map.of("name", "Widget B", "sales", 890)
                ),
                "totalRevenue", 125000
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // (125000 - 110000) / 110000 * 100 = 13.6%
        assertEquals(13.6, result.getOutputData().get("growthRate"));
    }

    @Test
    void computesHealthyScore() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("revenue", 125000, "orders", 3400, "avgOrderValue", 36.76, "returnRate", 0.03),
                "topProducts", List.of(Map.of("name", "Widget A", "sales", 1200)),
                "totalRevenue", 125000
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> insights = (Map<String, Object>) result.getOutputData().get("insights");
        assertEquals("HEALTHY", insights.get("healthScore"));
    }

    @Test
    void computesNeedsAttentionScore() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("revenue", 125000, "orders", 3400, "avgOrderValue", 36.76, "returnRate", 0.08),
                "topProducts", List.of(Map.of("name", "Widget A", "sales", 1200)),
                "totalRevenue", 125000
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> insights = (Map<String, Object>) result.getOutputData().get("insights");
        assertEquals("NEEDS ATTENTION", insights.get("healthScore"));
    }

    @Test
    void identifiesTopProduct() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("revenue", 125000, "orders", 3400, "avgOrderValue", 36.76, "returnRate", 0.03),
                "topProducts", List.of(
                        Map.of("name", "Widget A", "sales", 1200),
                        Map.of("name", "Widget B", "sales", 890)
                ),
                "totalRevenue", 125000
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> insights = (Map<String, Object>) result.getOutputData().get("insights");
        assertEquals("Widget A", insights.get("topProduct"));
    }

    @Test
    void includesEnrichedAtTimestamp() {
        Task task = taskWith(Map.of(
                "metrics", Map.of("revenue", 125000, "orders", 3400, "avgOrderValue", 36.76, "returnRate", 0.03),
                "topProducts", List.of(Map.of("name", "Widget A", "sales", 1200)),
                "totalRevenue", 125000
        ));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("enrichedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
