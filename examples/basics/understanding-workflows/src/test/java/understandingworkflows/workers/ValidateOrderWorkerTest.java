package understandingworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateOrderWorkerTest {

    private final ValidateOrderWorker worker = new ValidateOrderWorker();

    @Test
    void taskDefName() {
        assertEquals("validate_order", worker.getTaskDefName());
    }

    @Test
    void validatesItemsCorrectly() {
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Laptop", "price", 999.99, "qty", 1),
                Map.of("name", "Mouse", "price", 29.99, "qty", 2)
        );
        Task task = taskWith(Map.of("orderId", "ORD-1001", "items", items));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> validatedItems =
                (List<Map<String, Object>>) result.getOutputData().get("validatedItems");
        assertNotNull(validatedItems);
        assertEquals(2, validatedItems.size());

        for (Map<String, Object> item : validatedItems) {
            assertEquals(true, item.get("validated"));
            assertEquals(true, item.get("inStock"));
        }

        assertEquals("Laptop", validatedItems.get(0).get("name"));
        assertEquals("Mouse", validatedItems.get(1).get("name"));
    }

    @Test
    void handlesEmptyItems() {
        Task task = taskWith(Map.of("orderId", "ORD-1002", "items", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> validatedItems =
                (List<Map<String, Object>>) result.getOutputData().get("validatedItems");
        assertNotNull(validatedItems);
        assertEquals(0, validatedItems.size());
        assertEquals(0, result.getOutputData().get("itemCount"));
    }

    @Test
    void outputContainsRequiredFields() {
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Keyboard", "price", 49.99, "qty", 1)
        );
        Task task = taskWith(Map.of("orderId", "ORD-1003", "items", items));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("validatedItems"));
        assertNotNull(result.getOutputData().get("itemCount"));
        assertEquals(1, result.getOutputData().get("itemCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
