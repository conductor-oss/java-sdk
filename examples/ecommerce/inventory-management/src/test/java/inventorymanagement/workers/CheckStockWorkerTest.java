package inventorymanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckStockWorkerTest {

    private CheckStockWorker worker;

    @BeforeEach
    void setUp() {
        InventoryStore.reset();
        InventoryStore.seedDefaults();
        worker = new CheckStockWorker();
    }

    @Test
    void taskDefName() { assertEquals("inv_check_stock", worker.getTaskDefName()); }

    @Test
    void returnsAvailableQty() {
        Task task = taskWith(Map.of("sku", "WH-1000XM5", "warehouseId", "WH-EAST-01"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(45, ((Number) r.getOutputData().get("availableQty")).intValue());
    }

    @Test
    void returnsZeroForUnknownSku() {
        Task task = taskWith(Map.of("sku", "NONEXISTENT", "warehouseId", "WH-EAST-01"));
        TaskResult r = worker.execute(task);
        assertEquals(0, ((Number) r.getOutputData().get("availableQty")).intValue());
    }

    @Test
    void returnsTotalAcrossWarehouses() {
        Task task = taskWith(Map.of("sku", "WH-1000XM5", "warehouseId", "WH-EAST-01"));
        TaskResult r = worker.execute(task);
        // WH-EAST-01: 45, WH-WEST-01: 60 = 105
        assertEquals(105, ((Number) r.getOutputData().get("totalStockAllWarehouses")).intValue());
    }

    @Test
    void returnsLocation() {
        Task task = taskWith(Map.of("sku", "WH-1000XM5", "warehouseId", "WH-EAST-01"));
        TaskResult r = worker.execute(task);
        assertNotNull(r.getOutputData().get("location"));
        assertTrue(r.getOutputData().get("location").toString().contains("Aisle"));
    }

    @Test
    void lowStockWarning() {
        InventoryStore.setStock("WH-TEST", "LOW-SKU", 5);
        Task task = taskWith(Map.of("sku", "LOW-SKU", "warehouseId", "WH-TEST"));
        TaskResult r = worker.execute(task);
        assertEquals(true, r.getOutputData().get("lowStock"));
    }

    @Test
    void noLowStockForAdequateInventory() {
        Task task = taskWith(Map.of("sku", "WH-1000XM5", "warehouseId", "WH-EAST-01"));
        TaskResult r = worker.execute(task);
        assertEquals(false, r.getOutputData().get("lowStock"));
    }

    @Test
    void includesCheckedAt() {
        Task task = taskWith(Map.of("sku", "WH-1000XM5", "warehouseId", "WH-EAST-01"));
        TaskResult r = worker.execute(task);
        assertNotNull(r.getOutputData().get("checkedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
