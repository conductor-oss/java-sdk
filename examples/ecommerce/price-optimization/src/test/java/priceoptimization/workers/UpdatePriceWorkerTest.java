package priceoptimization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdatePriceWorkerTest {

    private final UpdatePriceWorker worker = new UpdatePriceWorker();

    @Test
    void taskDefName() {
        assertEquals("prz_update_price", worker.getTaskDefName());
    }

    @Test
    void computesPositiveChange() {
        Task task = taskWith(Map.of("currentPrice", 85.00, "newPrice", 87.38));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        double change = ((Number) result.getOutputData().get("priceChange")).doubleValue();
        assertEquals(2.38, change, 0.01);
    }

    @Test
    void computesNegativeChange() {
        Task task = taskWith(Map.of("currentPrice", 100.00, "newPrice", 98.00));
        TaskResult result = worker.execute(task);

        double change = ((Number) result.getOutputData().get("priceChange")).doubleValue();
        assertEquals(-2.00, change, 0.01);
    }

    @Test
    void computesZeroChange() {
        Task task = taskWith(Map.of("currentPrice", 50.00, "newPrice", 50.00));
        TaskResult result = worker.execute(task);

        double change = ((Number) result.getOutputData().get("priceChange")).doubleValue();
        assertEquals(0.0, change, 0.01);
    }

    @Test
    void returnsUpdatedFlag() {
        Task task = taskWith(Map.of("currentPrice", 85.00, "newPrice", 87.00));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("updated"));
    }

    @Test
    void returnsEffectiveAt() {
        Task task = taskWith(Map.of("currentPrice", 85.00, "newPrice", 87.00));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("effectiveAt"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("currentPrice", null);
        input.put("newPrice", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, ((Number) result.getOutputData().get("priceChange")).doubleValue(), 0.01);
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
