package workflowvariables.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalcPriceWorkerTest {

    private final CalcPriceWorker worker = new CalcPriceWorker();

    @Test
    void taskDefName() {
        assertEquals("wv_calc_price", worker.getTaskDefName());
    }

    @Test
    void calculatesSubtotalFromItems() {
        Task task = taskWith(Map.of("items", List.of(
                Map.of("name", "Laptop Stand", "price", 49.99, "qty", 1),
                Map.of("name", "USB Cable", "price", 12.99, "qty", 3)
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // 49.99 * 1 + 12.99 * 3 = 49.99 + 38.97 = 88.96
        assertEquals(88.96, ((Number) result.getOutputData().get("subtotal")).doubleValue(), 0.01);
        assertEquals(2, ((Number) result.getOutputData().get("itemCount")).intValue());
    }

    @Test
    void singleItem() {
        Task task = taskWith(Map.of("items", List.of(
                Map.of("name", "Widget", "price", 25.50, "qty", 2)
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(51.0, ((Number) result.getOutputData().get("subtotal")).doubleValue(), 0.01);
        assertEquals(1, ((Number) result.getOutputData().get("itemCount")).intValue());
    }

    @Test
    void emptyItemsList() {
        Task task = taskWith(Map.of("items", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, ((Number) result.getOutputData().get("subtotal")).doubleValue(), 0.01);
        assertEquals(0, ((Number) result.getOutputData().get("itemCount")).intValue());
    }

    @Test
    void nullItemsDefaultsToEmpty() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, ((Number) result.getOutputData().get("subtotal")).doubleValue(), 0.01);
        assertEquals(0, ((Number) result.getOutputData().get("itemCount")).intValue());
    }

    @Test
    void outputContainsSubtotalAndItemCount() {
        Task task = taskWith(Map.of("items", List.of(
                Map.of("name", "A", "price", 10.0, "qty", 1)
        )));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("subtotal"));
        assertNotNull(result.getOutputData().get("itemCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
