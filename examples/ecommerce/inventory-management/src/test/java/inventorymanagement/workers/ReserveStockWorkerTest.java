package inventorymanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReserveStockWorkerTest {

    private ReserveStockWorker worker;

    @BeforeEach
    void setUp() {
        InventoryStore.reset();
        InventoryStore.setStock("WH-EAST-01", "SKU-1", 45);
        InventoryStore.setStock("WH-EAST-01", "SKU-LOW", 3);
        worker = new ReserveStockWorker();
    }

    @Test
    void taskDefName() { assertEquals("inv_reserve", worker.getTaskDefName()); }

    @Test
    void reservesUpToAvailable() {
        Task task = taskWith(Map.of("sku", "SKU-1", "requestedQty", 20, "availableQty", 45,
                "warehouseId", "WH-EAST-01"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("reserved"));
        assertEquals(20, ((Number) r.getOutputData().get("reservedQty")).intValue());
        assertNotNull(r.getOutputData().get("reservationId"));
    }

    @Test
    void actuallyDeductsStock() {
        Task task = taskWith(Map.of("sku", "SKU-1", "requestedQty", 20, "availableQty", 45,
                "warehouseId", "WH-EAST-01"));
        worker.execute(task);
        // Stock should now be 45 - 20 = 25
        assertEquals(25, InventoryStore.getStock("WH-EAST-01", "SKU-1"));
    }

    @Test
    void partialReservationWithBackorder() {
        Task task = taskWith(Map.of("sku", "SKU-LOW", "requestedQty", 10, "availableQty", 3,
                "warehouseId", "WH-EAST-01"));
        TaskResult r = worker.execute(task);
        assertEquals(true, r.getOutputData().get("backordered"));
        int reserved = ((Number) r.getOutputData().get("reservedQty")).intValue();
        int backorderQty = ((Number) r.getOutputData().get("backorderQty")).intValue();
        assertEquals(10, reserved + backorderQty);
    }

    @Test
    void zeroAvailableNotReserved() {
        InventoryStore.setStock("WH-EAST-01", "EMPTY-SKU", 0);
        Task task = taskWith(Map.of("sku", "EMPTY-SKU", "requestedQty", 10, "availableQty", 0,
                "warehouseId", "WH-EAST-01"));
        TaskResult r = worker.execute(task);
        assertEquals(false, r.getOutputData().get("reserved"));
        assertEquals(true, r.getOutputData().get("backordered"));
    }

    @Test
    void reservationIdIsUnique() {
        Task t1 = taskWith(Map.of("sku", "SKU-1", "requestedQty", 5, "availableQty", 45,
                "warehouseId", "WH-EAST-01"));
        Task t2 = taskWith(Map.of("sku", "SKU-1", "requestedQty", 5, "availableQty", 40,
                "warehouseId", "WH-EAST-01"));
        String id1 = worker.execute(t1).getOutputData().get("reservationId").toString();
        String id2 = worker.execute(t2).getOutputData().get("reservationId").toString();
        assertNotEquals(id1, id2);
    }

    @Test
    void concurrentReservationsAreSafe() throws InterruptedException {
        InventoryStore.setStock("WH-EAST-01", "CONCURRENT-SKU", 100);

        // Run 10 concurrent reservations of 10 each
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                Task task = taskWith(Map.of("sku", "CONCURRENT-SKU", "requestedQty", 10,
                        "availableQty", 100, "warehouseId", "WH-EAST-01"));
                worker.execute(task);
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        // All 100 should be reserved, stock should be 0
        assertEquals(0, InventoryStore.getStock("WH-EAST-01", "CONCURRENT-SKU"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
