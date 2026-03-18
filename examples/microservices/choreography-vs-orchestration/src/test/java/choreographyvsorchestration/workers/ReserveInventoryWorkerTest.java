package choreographyvsorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReserveInventoryWorkerTest {

    @BeforeEach
    void resetInventory() {
        // Reset inventory before each test
        ReserveInventoryWorker.getInventory().clear();
        ReserveInventoryWorker.getInventory().put("widget", 500);
        ReserveInventoryWorker.getInventory().put("gadget", 200);
        ReserveInventoryWorker.getInventory().put("gizmo", 150);
    }

    @Test
    void reservesInventorySuccessfully() {
        ReserveInventoryWorker w = new ReserveInventoryWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "orderId", "ORD-001",
                "items", List.of("widget", "gadget")
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("reserved"));
        assertNotNull(r.getOutputData().get("warehouse"));
    }

    @Test
    void failsForItemNotInInventory() {
        ReserveInventoryWorker w = new ReserveInventoryWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "orderId", "ORD-002",
                "items", List.of("nonexistent_item")
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertEquals(false, r.getOutputData().get("reserved"));
        assertTrue(r.getReasonForIncompletion().contains("Insufficient inventory"));
    }

    @Test
    void failsWhenNoItemsProvided() {
        ReserveInventoryWorker w = new ReserveInventoryWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>());

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
    }

    @Test
    void decrementsInventory() {
        ReserveInventoryWorker.getInventory().put("widget", 10);

        ReserveInventoryWorker w = new ReserveInventoryWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "orderId", "ORD-003",
                "items", List.of("widget")
        )));

        w.execute(t);
        assertEquals(9, ReserveInventoryWorker.getInventory().get("widget"));
    }
}
