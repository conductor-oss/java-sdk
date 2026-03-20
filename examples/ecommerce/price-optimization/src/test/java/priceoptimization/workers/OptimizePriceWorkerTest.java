package priceoptimization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptimizePriceWorkerTest {

    private final OptimizePriceWorker worker = new OptimizePriceWorker();

    @Test
    void taskDefName() {
        assertEquals("prz_optimize_price", worker.getTaskDefName());
    }

    @Test
    void computesNewPriceWithHighDemand() {
        Task task = taskWith(Map.of("currentPrice", 85.00, "demandScore", 0.78));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        double newPrice = ((Number) result.getOutputData().get("newPrice")).doubleValue();
        // 85 * (1 + (0.78 - 0.5) * 0.1) = 85 * 1.028 = 87.38
        assertEquals(87.38, newPrice, 0.01);
    }

    @Test
    void computesNewPriceWithLowDemand() {
        Task task = taskWith(Map.of("currentPrice", 100.00, "demandScore", 0.3));
        TaskResult result = worker.execute(task);

        double newPrice = ((Number) result.getOutputData().get("newPrice")).doubleValue();
        // 100 * (1 + (0.3 - 0.5) * 0.1) = 100 * 0.98 = 98.0
        assertEquals(98.0, newPrice, 0.01);
    }

    @Test
    void computesAdjustmentPercent() {
        Task task = taskWith(Map.of("currentPrice", 85.00, "demandScore", 0.78));
        TaskResult result = worker.execute(task);

        double adj = ((Number) result.getOutputData().get("adjustmentPercent")).doubleValue();
        assertTrue(adj > 0);
    }

    @Test
    void returnsConfidence() {
        Task task = taskWith(Map.of("currentPrice", 50.00, "demandScore", 0.5));
        TaskResult result = worker.execute(task);

        assertEquals(0.87, result.getOutputData().get("confidence"));
    }

    @Test
    void handlesStringCurrentPrice() {
        Task task = taskWith(Map.of("currentPrice", "100.00", "demandScore", 0.5));
        TaskResult result = worker.execute(task);

        double newPrice = ((Number) result.getOutputData().get("newPrice")).doubleValue();
        assertEquals(100.00, newPrice, 0.01);
    }

    @Test
    void handlesNullInputsWithDefaults() {
        Map<String, Object> input = new HashMap<>();
        input.put("currentPrice", null);
        input.put("demandScore", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // defaults: 85 * (1 + (0.5-0.5)*0.1) = 85.0
        double newPrice = ((Number) result.getOutputData().get("newPrice")).doubleValue();
        assertEquals(85.0, newPrice, 0.01);
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("newPrice"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
