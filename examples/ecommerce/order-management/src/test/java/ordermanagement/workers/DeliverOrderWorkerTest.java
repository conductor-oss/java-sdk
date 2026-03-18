package ordermanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeliverOrderWorkerTest {

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
    void taskDefName() { assertEquals("ord_deliver", deliverWorker.getTaskDefName()); }

    @Test
    void deliversOrder() {
        String orderId = createFullPipeline();

        Task task = taskWith(Map.of("orderId", orderId, "trackingNumber", "TRK123456"));
        TaskResult r = deliverWorker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("delivered"));
        assertNotNull(r.getOutputData().get("signedBy"));
    }

    @Test
    void transitionsToDelivered() {
        String orderId = createFullPipeline();

        Task task = taskWith(Map.of("orderId", orderId, "trackingNumber", "TRK789"));
        deliverWorker.execute(task);

        assertEquals("DELIVERED", OrderStore.getStatus(orderId));
    }

    @Test
    void includesDeliveredAt() {
        String orderId = createFullPipeline();

        Task task = taskWith(Map.of("orderId", orderId, "trackingNumber", "TRK999"));
        TaskResult r = deliverWorker.execute(task);
        assertNotNull(r.getOutputData().get("deliveredAt"));
    }

    @Test
    void includesOrderHistory() {
        String orderId = createFullPipeline();

        Task task = taskWith(Map.of("orderId", orderId, "trackingNumber", "TRK-HIST"));
        TaskResult r = deliverWorker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) r.getOutputData().get("orderHistory");
        assertNotNull(history);
        // Should have: CREATED (initial), CREATED->CONFIRMED, CONFIRMED->PROCESSING,
        // PROCESSING->SHIPPED, SHIPPED->DELIVERED = 5 entries
        assertTrue(history.size() >= 4, "Should have at least 4 history entries, got " + history.size());
    }

    @Test
    void fullStateMachineTransitions() {
        String orderId = createFullPipeline();

        // Verify the full state transition path
        List<Map<String, Object>> history = OrderStore.getHistory(orderId);

        // Check we went through all states
        Set<String> states = new HashSet<>();
        for (Map<String, Object> entry : history) {
            if (entry.get("state") != null) states.add((String) entry.get("state"));
            if (entry.get("from") != null) states.add((String) entry.get("from"));
            if (entry.get("to") != null) states.add((String) entry.get("to"));
        }
        assertTrue(states.contains("CREATED"));
        assertTrue(states.contains("CONFIRMED"));
        assertTrue(states.contains("PROCESSING"));
        assertTrue(states.contains("SHIPPED"));
    }

    @Test
    void cannotDeliverFromCreatedState() {
        // Create order but don't go through the pipeline
        Task createTask = new Task();
        createTask.setStatus(Task.Status.IN_PROGRESS);
        createTask.setInputData(new HashMap<>(Map.of("customerId", "cust-x",
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1)))));
        String orderId = createWorker.execute(createTask).getOutputData().get("orderId").toString();

        Task task = taskWith(Map.of("orderId", orderId, "trackingNumber", "TRK-BAD"));
        TaskResult r = deliverWorker.execute(task);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertEquals(false, r.getOutputData().get("delivered"));
    }

    private String createFullPipeline() {
        // Create
        Task createTask = new Task();
        createTask.setStatus(Task.Status.IN_PROGRESS);
        createTask.setInputData(new HashMap<>(Map.of("customerId", "cust-" + System.nanoTime(),
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1)))));
        String orderId = createWorker.execute(createTask).getOutputData().get("orderId").toString();

        // Validate (CREATED -> CONFIRMED)
        Task valTask = new Task();
        valTask.setStatus(Task.Status.IN_PROGRESS);
        valTask.setInputData(new HashMap<>(Map.of("orderId", orderId,
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1)))));
        validateWorker.execute(valTask);

        // Fulfill (CONFIRMED -> PROCESSING)
        Task fulTask = new Task();
        fulTask.setStatus(Task.Status.IN_PROGRESS);
        fulTask.setInputData(new HashMap<>(Map.of("orderId", orderId,
                "items", List.of(Map.of("sku", "A", "qty", 1)), "warehouseId", "WH-1")));
        fulfillWorker.execute(fulTask);

        // Ship (PROCESSING -> SHIPPED)
        Task shipTask = new Task();
        shipTask.setStatus(Task.Status.IN_PROGRESS);
        shipTask.setInputData(new HashMap<>(Map.of("orderId", orderId, "fulfillmentId", "FUL-1",
                "shippingAddress", Map.of("city", "Austin", "state", "TX"),
                "shippingMethod", "standard")));
        shipWorker.execute(shipTask);

        return orderId;
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
