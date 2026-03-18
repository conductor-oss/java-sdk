package inventorymanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateInventoryWorkerTest {

    private UpdateInventoryWorker worker;

    @BeforeEach
    void setUp() {
        InventoryStore.reset();
        InventoryStore.setStock("WH-1", "SKU-1", 45);
        worker = new UpdateInventoryWorker();
    }

    @Test
    void taskDefName() { assertEquals("inv_update", worker.getTaskDefName()); }

    @Test
    void reportsRemainingAfterReservation() {
        // Simulate: reserve 20 of 45 via InventoryStore first
        InventoryStore.reserve("WH-1", "SKU-1", 20, "test-res-1");

        Task task = taskWith(Map.of("sku", "SKU-1", "warehouseId", "WH-1",
                "previousQty", 45, "reservedQty", 20, "reservationId", "test-res-1"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        // After reserving 20 from 45, remaining should be 25
        assertEquals(25, ((Number) r.getOutputData().get("remainingQty")).intValue());
    }

    @Test
    void returnsUpdatedAt() {
        Task task = taskWith(Map.of("sku", "SKU-1", "warehouseId", "WH-1",
                "previousQty", 45, "reservedQty", 5));
        TaskResult r = worker.execute(task);
        assertNotNull(r.getOutputData().get("updatedAt"));
    }

    @Test
    void includesVerificationFlag() {
        Task task = taskWith(Map.of("sku", "SKU-1", "warehouseId", "WH-1",
                "previousQty", 45, "reservedQty", 0));
        TaskResult r = worker.execute(task);
        assertNotNull(r.getOutputData().get("verified"));
    }

    @Test
    void includesDelta() {
        InventoryStore.reserve("WH-1", "SKU-1", 10, "test-res-2");

        Task task = taskWith(Map.of("sku", "SKU-1", "warehouseId", "WH-1",
                "previousQty", 45, "reservedQty", 10, "reservationId", "test-res-2"));
        TaskResult r = worker.execute(task);
        int delta = ((Number) r.getOutputData().get("delta")).intValue();
        assertEquals(-10, delta);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
