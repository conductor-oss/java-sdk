package forkjoin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GetInventoryWorkerTest {

    private final GetInventoryWorker worker = new GetInventoryWorker();

    @Test
    void taskDefName() {
        assertEquals("fj_get_inventory", worker.getTaskDefName());
    }

    @Test
    void returnsInventoryDetails() {
        Task task = taskWith(Map.of("productId", "PROD-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> inventory = (Map<String, Object>) result.getOutputData().get("inventory");
        assertNotNull(inventory);
        assertEquals("PROD-001", inventory.get("productId"));
        assertNotNull(inventory.get("inStock"));
        assertNotNull(inventory.get("quantity"));
        assertNotNull(inventory.get("warehouse"));
    }

    @Test
    void quantityIsDeterministic() {
        Task task1 = taskWith(Map.of("productId", "PROD-001"));
        Task task2 = taskWith(Map.of("productId", "PROD-001"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        @SuppressWarnings("unchecked")
        Map<String, Object> inv1 = (Map<String, Object>) result1.getOutputData().get("inventory");
        @SuppressWarnings("unchecked")
        Map<String, Object> inv2 = (Map<String, Object>) result2.getOutputData().get("inventory");

        assertEquals(inv1.get("quantity"), inv2.get("quantity"));
        assertEquals(inv1.get("warehouse"), inv2.get("warehouse"));
    }

    @Test
    void differentProductsGetDifferentInventory() {
        Task task1 = taskWith(Map.of("productId", "PROD-001"));
        Task task2 = taskWith(Map.of("productId", "PROD-002"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        @SuppressWarnings("unchecked")
        Map<String, Object> inv1 = (Map<String, Object>) result1.getOutputData().get("inventory");
        @SuppressWarnings("unchecked")
        Map<String, Object> inv2 = (Map<String, Object>) result2.getOutputData().get("inventory");

        assertEquals("PROD-001", inv1.get("productId"));
        assertEquals("PROD-002", inv2.get("productId"));
    }

    @Test
    void failsOnMissingProductId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("productId"));
    }

    @Test
    void failsOnBlankProductId() {
        Task task = taskWith(Map.of("productId", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("productId"));
    }

    @Test
    void failsOnNullProductId() {
        Map<String, Object> input = new HashMap<>();
        input.put("productId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void inventoryContainsAllExpectedFields() {
        Task task = taskWith(Map.of("productId", "PROD-001"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> inventory = (Map<String, Object>) result.getOutputData().get("inventory");
        assertEquals(4, inventory.size());
        assertTrue(inventory.containsKey("productId"));
        assertTrue(inventory.containsKey("inStock"));
        assertTrue(inventory.containsKey("quantity"));
        assertTrue(inventory.containsKey("warehouse"));
    }

    @Test
    void inStockMatchesQuantity() {
        Task task = taskWith(Map.of("productId", "PROD-001"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> inventory = (Map<String, Object>) result.getOutputData().get("inventory");

        int quantity = (int) inventory.get("quantity");
        boolean inStock = (boolean) inventory.get("inStock");
        assertEquals(quantity > 0, inStock);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
