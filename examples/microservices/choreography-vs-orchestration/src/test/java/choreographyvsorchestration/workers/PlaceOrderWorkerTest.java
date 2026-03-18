package choreographyvsorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlaceOrderWorkerTest {

    @Test
    void executesSuccessfullyWithSimpleItems() {
        PlaceOrderWorker w = new PlaceOrderWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "orderId", "ORD-001",
                "customerId", "CUST-1",
                "items", List.of("widget", "gadget")
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("placed", r.getOutputData().get("status"));
        assertEquals("ORD-001", r.getOutputData().get("orderId"));
        assertEquals(59.98, r.getOutputData().get("total")); // 2 simple items * $29.99
        assertEquals(2, r.getOutputData().get("itemCount"));
    }

    @Test
    void executesSuccessfullyWithDetailedItems() {
        PlaceOrderWorker w = new PlaceOrderWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "orderId", "ORD-002",
                "customerId", "CUST-2",
                "items", List.of(
                        Map.of("name", "widget", "price", 10.0, "quantity", 3),
                        Map.of("name", "gadget", "price", 25.0, "quantity", 2)
                )
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(80.0, r.getOutputData().get("total")); // 10*3 + 25*2
        assertEquals(5, r.getOutputData().get("totalQuantity"));
    }

    @Test
    void failsWithMissingOrderId() {
        PlaceOrderWorker w = new PlaceOrderWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "customerId", "CUST-1",
                "items", List.of("widget")
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("orderId"));
    }

    @Test
    void failsWithEmptyItems() {
        PlaceOrderWorker w = new PlaceOrderWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "orderId", "ORD-003",
                "customerId", "CUST-1",
                "items", List.of()
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("at least one item"));
    }
}
