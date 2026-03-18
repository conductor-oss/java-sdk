package inventorymanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReorderWorkerTest {

    private ReorderWorker worker;

    @BeforeEach
    void setUp() {
        InventoryStore.reset();
        worker = new ReorderWorker();
    }

    @Test
    void taskDefName() { assertEquals("inv_reorder", worker.getTaskDefName()); }

    @Test
    void reordersWhenBelowThreshold() {
        Task task = taskWith(Map.of("sku", "SKU-1", "remainingQty", 25, "reorderThreshold", 30));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("reorderPlaced"));
        // reorderQty = max(30*2, 50) = 60
        int reorderQty = ((Number) r.getOutputData().get("reorderQty")).intValue();
        assertTrue(reorderQty >= 50, "Reorder qty should be at least 50");
        assertNotNull(r.getOutputData().get("poNumber"));
    }

    @Test
    void noReorderAboveThreshold() {
        Task task = taskWith(Map.of("sku", "SKU-1", "remainingQty", 50, "reorderThreshold", 30));
        TaskResult r = worker.execute(task);
        assertEquals(false, r.getOutputData().get("reorderPlaced"));
        assertEquals(0, ((Number) r.getOutputData().get("reorderQty")).intValue());
        assertNull(r.getOutputData().get("poNumber"));
    }

    @Test
    void reorderAtExactThreshold() {
        Task task = taskWith(Map.of("sku", "SKU-1", "remainingQty", 30, "reorderThreshold", 30));
        TaskResult r = worker.execute(task);
        assertEquals(true, r.getOutputData().get("reorderPlaced"));
    }

    @Test
    void rushPriorityWhenZeroStock() {
        Task task = taskWith(Map.of("sku", "SKU-1", "remainingQty", 0, "reorderThreshold", 30));
        TaskResult r = worker.execute(task);
        assertEquals(true, r.getOutputData().get("reorderPlaced"));
        assertEquals("rush", r.getOutputData().get("priority"));
    }

    @Test
    void standardPriorityWhenLowStock() {
        Task task = taskWith(Map.of("sku", "SKU-1", "remainingQty", 10, "reorderThreshold", 30));
        TaskResult r = worker.execute(task);
        assertEquals("standard", r.getOutputData().get("priority"));
    }

    @Test
    void includesEstimatedArrival() {
        Task task = taskWith(Map.of("sku", "SKU-1", "remainingQty", 5, "reorderThreshold", 30));
        TaskResult r = worker.execute(task);
        assertNotNull(r.getOutputData().get("estimatedArrival"));
    }

    @Test
    void includesBackorderQuantityInReorder() {
        // Add some backorders
        InventoryStore.addBackorder("SKU-BACK", 10, "order-1");
        InventoryStore.addBackorder("SKU-BACK", 15, "order-2");

        Task task = taskWith(Map.of("sku", "SKU-BACK", "remainingQty", 5, "reorderThreshold", 30));
        TaskResult r = worker.execute(task);
        // reorderQty should include backorder qty: max(30*2, 50) + 25 = 85
        int reorderQty = ((Number) r.getOutputData().get("reorderQty")).intValue();
        assertTrue(reorderQty >= 75, "Should include backorder qty in reorder");
        assertEquals(25, ((Number) r.getOutputData().get("pendingBackorders")).intValue());
    }

    @Test
    void poNumberIsUnique() {
        Task t1 = taskWith(Map.of("sku", "SKU-1", "remainingQty", 5, "reorderThreshold", 30));
        Task t2 = taskWith(Map.of("sku", "SKU-2", "remainingQty", 3, "reorderThreshold", 30));
        String po1 = worker.execute(t1).getOutputData().get("poNumber").toString();
        String po2 = worker.execute(t2).getOutputData().get("poNumber").toString();
        assertNotEquals(po1, po2);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
