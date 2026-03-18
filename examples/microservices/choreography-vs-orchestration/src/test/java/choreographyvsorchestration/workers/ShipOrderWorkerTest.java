package choreographyvsorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShipOrderWorkerTest {

    @Test
    void shipsSuccessfully() {
        ShipOrderWorker w = new ShipOrderWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "orderId", "ORD-001",
                "warehouse", "WH-CENTRAL",
                "customerId", "CUST-1"
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("shipped"));
        assertNotNull(r.getOutputData().get("trackingId"));
        assertTrue(((String) r.getOutputData().get("trackingId")).startsWith("TRACK-"));
        assertNotNull(r.getOutputData().get("estimatedDelivery"));
        assertEquals(2, r.getOutputData().get("deliveryDays"));
        assertEquals("express", r.getOutputData().get("carrier"));
    }

    @Test
    void failsWithMissingOrderId() {
        ShipOrderWorker w = new ShipOrderWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("warehouse", "WH-EAST")));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("orderId"));
    }

    @Test
    void failsWithMissingWarehouse() {
        ShipOrderWorker w = new ShipOrderWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("orderId", "ORD-002")));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("warehouse"));
    }

    @Test
    void usesEconomyForUnknownWarehouse() {
        ShipOrderWorker w = new ShipOrderWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "orderId", "ORD-003",
                "warehouse", "WH-REMOTE"
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(5, r.getOutputData().get("deliveryDays"));
        assertEquals("economy", r.getOutputData().get("carrier"));
    }

    @Test
    void generatesUniqueTrackingIds() {
        ShipOrderWorker w = new ShipOrderWorker();

        Task t1 = new Task();
        t1.setStatus(Task.Status.IN_PROGRESS);
        t1.setInputData(new HashMap<>(Map.of("orderId", "ORD-A", "warehouse", "WH-EAST")));

        Task t2 = new Task();
        t2.setStatus(Task.Status.IN_PROGRESS);
        t2.setInputData(new HashMap<>(Map.of("orderId", "ORD-B", "warehouse", "WH-EAST")));

        TaskResult r1 = w.execute(t1);
        TaskResult r2 = w.execute(t2);

        assertNotEquals(r1.getOutputData().get("trackingId"), r2.getOutputData().get("trackingId"));
    }
}
