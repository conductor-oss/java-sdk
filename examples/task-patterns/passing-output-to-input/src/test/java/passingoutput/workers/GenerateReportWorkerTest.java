package passingoutput.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateReportWorkerTest {

    private final GenerateReportWorker worker = new GenerateReportWorker();

    @Test
    void taskDefName() {
        assertEquals("generate_report", worker.getTaskDefName());
    }

    @Test
    void generatesMetricsObject() {
        Task task = taskWith(Map.of("region", "US-West", "period", "Q1-2026"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("metrics");
        assertNotNull(metrics);
        assertEquals(125000, metrics.get("revenue"));
        assertEquals(3400, metrics.get("orders"));
        assertEquals(36.76, metrics.get("avgOrderValue"));
        assertEquals(0.03, metrics.get("returnRate"));
    }

    @Test
    void generatesTopProductsArray() {
        Task task = taskWith(Map.of("region", "US-West", "period", "Q1-2026"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topProducts = (List<Map<String, Object>>) result.getOutputData().get("topProducts");
        assertNotNull(topProducts);
        assertEquals(3, topProducts.size());
        assertEquals("Widget A", topProducts.get(0).get("name"));
        assertEquals(1200, topProducts.get(0).get("sales"));
    }

    @Test
    void includesGeneratedAtTimestamp() {
        Task task = taskWith(Map.of("region", "US-West", "period", "Q1-2026"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("generatedAt"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("region", "US-West", "period", "Q1-2026"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("metrics"));
        assertTrue(result.getOutputData().containsKey("topProducts"));
        assertTrue(result.getOutputData().containsKey("generatedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
