package workflowvariables.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuildSummaryWorkerTest {

    private final BuildSummaryWorker worker = new BuildSummaryWorker();

    @Test
    void taskDefName() {
        assertEquals("wv_build_summary", worker.getTaskDefName());
    }

    @Test
    void buildsSummaryFromAllInputs() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-5001",
                "customerTier", "gold",
                "subtotal", 88.96,
                "discount", 17.79,
                "discountRate", 0.20,
                "shipping", 0.0,
                "finalTotal", 71.17
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertTrue(summary.contains("ORD-5001"));
        assertTrue(summary.contains("gold"));
        assertTrue(summary.contains("88.96"));
        assertTrue(summary.contains("17.79"));
        assertTrue(summary.contains("20%"));
        assertTrue(summary.contains("71.17"));
    }

    @Test
    void summaryIncludesOrderIdAndTier() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-9999",
                "customerTier", "silver",
                "subtotal", 100.0,
                "discount", 10.0,
                "discountRate", 0.10,
                "shipping", 5.99,
                "finalTotal", 95.99
        ));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("ORD-9999"));
        assertTrue(summary.contains("silver tier"));
    }

    @Test
    void summaryIncludesDiscountPercentage() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-1",
                "customerTier", "bronze",
                "subtotal", 200.0,
                "discount", 10.0,
                "discountRate", 0.05,
                "shipping", 0.0,
                "finalTotal", 190.0
        ));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("5%"));
    }

    @Test
    void summaryIsMultiLine() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-1",
                "customerTier", "gold",
                "subtotal", 50.0,
                "discount", 10.0,
                "discountRate", 0.20,
                "shipping", 0.0,
                "finalTotal", 40.0
        ));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        String[] lines = summary.split("\n");
        assertTrue(lines.length >= 5, "Summary should have at least 5 lines");
    }

    @Test
    void outputContainsSummaryField() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-1",
                "customerTier", "gold",
                "subtotal", 50.0,
                "discount", 10.0,
                "discountRate", 0.20,
                "shipping", 0.0,
                "finalTotal", 40.0
        ));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("summary"));
        assertInstanceOf(String.class, result.getOutputData().get("summary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
