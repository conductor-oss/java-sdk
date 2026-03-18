package understandingworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculateTotalWorkerTest {

    private final CalculateTotalWorker worker = new CalculateTotalWorker();

    @Test
    void taskDefName() {
        assertEquals("calculate_total", worker.getTaskDefName());
    }

    @Test
    void calculatesTotalCorrectly() {
        // Laptop: 999.99 * 1 = 999.99, Mouse: 29.99 * 2 = 59.98
        // Subtotal = 1059.97, Tax = 84.80, Total = 1144.77
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Laptop", "price", 999.99, "qty", 1, "validated", true, "inStock", true),
                Map.of("name", "Mouse", "price", 29.99, "qty", 2, "validated", true, "inStock", true)
        );
        Task task = taskWith(Map.of("validatedItems", items));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1059.97, (double) result.getOutputData().get("subtotal"), 0.01);
        assertEquals(84.80, (double) result.getOutputData().get("tax"), 0.01);
        assertEquals(1144.77, (double) result.getOutputData().get("total"), 0.01);
    }

    @Test
    void taxCalculation() {
        // Single item: 100.00 * 1 = 100.00
        // Tax at 8% = 8.00, Total = 108.00
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Widget", "price", 100.00, "qty", 1, "validated", true, "inStock", true)
        );
        Task task = taskWith(Map.of("validatedItems", items));
        TaskResult result = worker.execute(task);

        assertEquals(100.00, (double) result.getOutputData().get("subtotal"), 0.01);
        assertEquals(8.00, (double) result.getOutputData().get("tax"), 0.01);
        assertEquals(108.00, (double) result.getOutputData().get("total"), 0.01);
    }

    @Test
    void handlesSingleItem() {
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Book", "price", 19.99, "qty", 3, "validated", true, "inStock", true)
        );
        Task task = taskWith(Map.of("validatedItems", items));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // 19.99 * 3 = 59.97
        assertEquals(59.97, (double) result.getOutputData().get("subtotal"), 0.01);
        assertNotNull(result.getOutputData().get("tax"));
        assertNotNull(result.getOutputData().get("total"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
