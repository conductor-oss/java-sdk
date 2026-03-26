package ordermanagement;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import ordermanagement.workers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full order management workflow data flow.
 * Tests: create -> validate -> fulfill -> ship -> deliver, and cancellation path.
 */
@SuppressWarnings("unchecked")
class OrderManagementIntegrationTest {

    private CreateOrderWorker createWorker;
    private ValidateOrderWorker validateWorker;
    private FulfillOrderWorker fulfillWorker;
    private ShipOrderWorker shipWorker;
    private DeliverOrderWorker deliverWorker;

    @BeforeEach
    void setUp() {
        OrderStore.reset();
        createWorker = new CreateOrderWorker();
        validateWorker = new ValidateOrderWorker();
        fulfillWorker = new FulfillOrderWorker();
        shipWorker = new ShipOrderWorker();
        deliverWorker = new DeliverOrderWorker();
    }

    @Test
    void fullLifecycle_createToDelivery() {
        List<Map<String, Object>> items = List.of(
                Map.of("sku", "LAPTOP-PRO", "name", "Laptop Pro 16", "price", 1999.00, "qty", 1),
                Map.of("sku", "USB-C-HUB", "name", "USB-C Hub", "price", 49.99, "qty", 2));

        // Step 1: Create
        TaskResult createResult = exec(createWorker, Map.of(
                "customerId", "cust-integ-1", "items", items));
        assertEquals(TaskResult.Status.COMPLETED, createResult.getStatus());
        String orderId = createResult.getOutputData().get("orderId").toString();
        assertEquals("CREATED", createResult.getOutputData().get("status"));
        assertEquals("2098.98", createResult.getOutputData().get("total"));

        // Step 2: Validate (CREATED -> CONFIRMED)
        TaskResult valResult = exec(validateWorker, Map.of("orderId", orderId, "items", items));
        assertEquals(TaskResult.Status.COMPLETED, valResult.getStatus());
        assertEquals(true, valResult.getOutputData().get("valid"));
        assertEquals("CONFIRMED", valResult.getOutputData().get("status"));
        assertEquals("CREATED", valResult.getOutputData().get("previousStatus"));

        // Step 3: Fulfill (CONFIRMED -> PROCESSING)
        TaskResult fulResult = exec(fulfillWorker, Map.of("orderId", orderId,
                "items", items, "warehouseId", "WH-EAST-01"));
        assertEquals(TaskResult.Status.COMPLETED, fulResult.getStatus());
        assertEquals("PROCESSING", fulResult.getOutputData().get("status"));
        String fulfillmentId = fulResult.getOutputData().get("fulfillmentId").toString();
        assertNotNull(fulfillmentId);

        // Step 4: Ship (PROCESSING -> SHIPPED)
        TaskResult shipResult = exec(shipWorker, Map.of("orderId", orderId,
                "fulfillmentId", fulfillmentId,
                "shippingAddress", Map.of("city", "Austin", "state", "TX", "zip", "73301"),
                "shippingMethod", "express"));
        assertEquals(TaskResult.Status.COMPLETED, shipResult.getStatus());
        assertEquals("SHIPPED", shipResult.getOutputData().get("status"));
        String trackingNumber = shipResult.getOutputData().get("trackingNumber").toString();
        assertEquals("FedEx", shipResult.getOutputData().get("carrier"));

        // Step 5: Deliver (SHIPPED -> DELIVERED)
        TaskResult delResult = exec(deliverWorker, Map.of("orderId", orderId,
                "trackingNumber", trackingNumber));
        assertEquals(TaskResult.Status.COMPLETED, delResult.getStatus());
        assertEquals(true, delResult.getOutputData().get("delivered"));
        assertEquals("DELIVERED", delResult.getOutputData().get("status"));

        // Verify full history
        List<Map<String, Object>> history = (List<Map<String, Object>>) delResult.getOutputData().get("orderHistory");
        assertNotNull(history);
        // History should have entries for: CREATED->CONFIRMED, CONFIRMED->PROCESSING, PROCESSING->SHIPPED, SHIPPED->DELIVERED
        assertTrue(history.size() >= 4, "History should have at least 4 transitions, got " + history.size());
    }

    @Test
    void orderCancellation_fromCreatedState() {
        List<Map<String, Object>> items = List.of(
                Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1));

        // Create
        TaskResult createResult = exec(createWorker, Map.of("customerId", "cust-cancel", "items", items));
        String orderId = createResult.getOutputData().get("orderId").toString();
        assertEquals("CREATED", OrderStore.getStatus(orderId));

        // Cancel directly
        boolean cancelled = OrderStore.transition(orderId, "CANCELLED", "test", "Customer requested cancellation");
        assertTrue(cancelled, "Should be able to cancel from CREATED state");
        assertEquals("CANCELLED", OrderStore.getStatus(orderId));

        // Validate should fail after cancellation (cannot transition from CANCELLED)
        TaskResult valResult = exec(validateWorker, Map.of("orderId", orderId, "items", items));
        assertEquals(TaskResult.Status.FAILED, valResult.getStatus());
    }

    @Test
    void orderCancellation_fromConfirmedState() {
        List<Map<String, Object>> items = List.of(
                Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1));

        // Create + Validate
        TaskResult createResult = exec(createWorker, Map.of("customerId", "cust-cancel2", "items", items));
        String orderId = createResult.getOutputData().get("orderId").toString();
        exec(validateWorker, Map.of("orderId", orderId, "items", items));
        assertEquals("CONFIRMED", OrderStore.getStatus(orderId));

        // Cancel
        boolean cancelled = OrderStore.transition(orderId, "CANCELLED", "test", "Customer cancelled after confirmation");
        assertTrue(cancelled);
        assertEquals("CANCELLED", OrderStore.getStatus(orderId));

        // Fulfill should fail
        TaskResult fulResult = exec(fulfillWorker, Map.of("orderId", orderId,
                "items", items, "warehouseId", "WH-1"));
        assertEquals(TaskResult.Status.FAILED, fulResult.getStatus());
    }

    @Test
    void invalidStateTransition_shipBeforeFulfill() {
        List<Map<String, Object>> items = List.of(
                Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1));

        TaskResult createResult = exec(createWorker, Map.of("customerId", "cust-skip", "items", items));
        String orderId = createResult.getOutputData().get("orderId").toString();
        exec(validateWorker, Map.of("orderId", orderId, "items", items));

        // Try to ship without fulfilling first (CONFIRMED -> SHIPPED is invalid)
        TaskResult shipResult = exec(shipWorker, Map.of("orderId", orderId,
                "fulfillmentId", "FUL-FAKE", "shippingMethod", "standard"));
        assertEquals(TaskResult.Status.FAILED, shipResult.getStatus());
        assertTrue(shipResult.getReasonForIncompletion().contains("Invalid state transition"));
    }

    @Test
    void invalidInput_missingOrderId_failsTerminally() {
        TaskResult result = exec(validateWorker, Map.of("items", List.of(Map.of("sku", "A", "qty", 1))));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    private TaskResult exec(com.netflix.conductor.client.worker.Worker worker, Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return worker.execute(task);
    }
}
