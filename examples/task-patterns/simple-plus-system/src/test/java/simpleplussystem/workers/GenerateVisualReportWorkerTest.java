package simpleplussystem.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateVisualReportWorkerTest {

    private final GenerateVisualReportWorker worker = new GenerateVisualReportWorker();

    @Test
    void taskDefName() {
        assertEquals("generate_visual_report", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of(
                "storeId", "STORE-42",
                "stats", Map.of("totalRevenue", 814, "avgOrderValue", 163, "maxOrder", 320, "orderCount", 5)
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsChartUrl() {
        Task task = taskWith(Map.of(
                "storeId", "STORE-42",
                "stats", Map.of("totalRevenue", 814, "avgOrderValue", 163)
        ));
        TaskResult result = worker.execute(task);

        String chartUrl = (String) result.getOutputData().get("chartUrl");
        assertNotNull(chartUrl);
        assertTrue(chartUrl.contains("STORE-42"), "Chart URL should contain store ID");
        assertTrue(chartUrl.startsWith("https://"), "Chart URL should be HTTPS");
    }

    @Test
    void returnsGeneratedAtTimestamp() {
        Task task = taskWith(Map.of(
                "storeId", "STORE-1",
                "stats", Map.of("totalRevenue", 500, "avgOrderValue", 100)
        ));
        TaskResult result = worker.execute(task);

        String generatedAt = (String) result.getOutputData().get("generatedAt");
        assertNotNull(generatedAt, "Should include a generatedAt timestamp");
        assertFalse(generatedAt.isBlank());
    }

    @Test
    void handlesNullStats() {
        Map<String, Object> input = new HashMap<>();
        input.put("storeId", "STORE-1");
        input.put("stats", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("chartUrl"));
    }

    @Test
    void chartUrlContainsStoreId() {
        Task task = taskWith(Map.of(
                "storeId", "MY-STORE",
                "stats", Map.of("totalRevenue", 100, "avgOrderValue", 50)
        ));
        TaskResult result = worker.execute(task);

        String chartUrl = (String) result.getOutputData().get("chartUrl");
        assertTrue(chartUrl.contains("MY-STORE"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
