package ordermanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ShipOrderWorkerTest {

    private CreateOrderWorker createWorker;
    private ValidateOrderWorker validateWorker;
    private FulfillOrderWorker fulfillWorker;
    private ShipOrderWorker shipWorker;

    @BeforeEach
    void setUp() {
        OrderStore.reset();
        createWorker = new CreateOrderWorker();
        validateWorker = new ValidateOrderWorker();
        fulfillWorker = new FulfillOrderWorker();
        shipWorker = new ShipOrderWorker();
    }

    @Test
    void taskDefName() { assertEquals("ord_ship", shipWorker.getTaskDefName()); }

    @Test
    void returnsTrackingNumber() {
        String orderId = createConfirmAndFulfill();

        Task task = taskWith(Map.of("orderId", orderId, "fulfillmentId", "FUL-1",
                "shippingAddress", Map.of("city", "Austin", "state", "TX", "zip", "73301"),
                "shippingMethod", "express"));
        TaskResult r = shipWorker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("trackingNumber"));
    }

    @Test
    void expressUsesFedEx() {
        String orderId = createConfirmAndFulfill();

        Task task = taskWith(Map.of("orderId", orderId, "fulfillmentId", "FUL-1",
                "shippingAddress", Map.of("city", "NYC", "state", "NY"),
                "shippingMethod", "express"));
        TaskResult r = shipWorker.execute(task);
        assertEquals("FedEx", r.getOutputData().get("carrier"));
        assertEquals(3, ((Number) r.getOutputData().get("transitDays")).intValue());
    }

    @Test
    void standardUsesUSPS() {
        String orderId = createConfirmAndFulfill();

        Task task = taskWith(Map.of("orderId", orderId, "fulfillmentId", "FUL-2",
                "shippingAddress", Map.of("city", "LA", "state", "CA"),
                "shippingMethod", "standard"));
        TaskResult r = shipWorker.execute(task);
        assertEquals("USPS", r.getOutputData().get("carrier"));
    }

    @Test
    void overnightUsesUPS() {
        String orderId = createConfirmAndFulfill();

        Task task = taskWith(Map.of("orderId", orderId, "fulfillmentId", "FUL-3",
                "shippingAddress", Map.of("city", "Chicago", "state", "IL"),
                "shippingMethod", "overnight"));
        TaskResult r = shipWorker.execute(task);
        assertEquals("UPS", r.getOutputData().get("carrier"));
        assertEquals(1, ((Number) r.getOutputData().get("transitDays")).intValue());
    }

    @Test
    void transitionsToShipped() {
        String orderId = createConfirmAndFulfill();

        Task task = taskWith(Map.of("orderId", orderId, "fulfillmentId", "FUL-1",
                "shippingAddress", Map.of("city", "Austin"),
                "shippingMethod", "standard"));
        shipWorker.execute(task);

        assertEquals("SHIPPED", OrderStore.getStatus(orderId));
    }

    @Test
    void includesEstimatedDelivery() {
        String orderId = createConfirmAndFulfill();

        Task task = taskWith(Map.of("orderId", orderId, "fulfillmentId", "FUL-1",
                "shippingAddress", Map.of("city", "Austin"),
                "shippingMethod", "express"));
        TaskResult r = shipWorker.execute(task);
        assertNotNull(r.getOutputData().get("estimatedDelivery"));
    }

    private String createConfirmAndFulfill() {
        // Create
        Task createTask = new Task();
        createTask.setStatus(Task.Status.IN_PROGRESS);
        createTask.setInputData(new HashMap<>(Map.of("customerId", "cust-" + System.nanoTime(),
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1)))));
        String orderId = createWorker.execute(createTask).getOutputData().get("orderId").toString();

        // Validate
        Task valTask = new Task();
        valTask.setStatus(Task.Status.IN_PROGRESS);
        valTask.setInputData(new HashMap<>(Map.of("orderId", orderId,
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1)))));
        validateWorker.execute(valTask);

        // Fulfill
        Task fulTask = new Task();
        fulTask.setStatus(Task.Status.IN_PROGRESS);
        fulTask.setInputData(new HashMap<>(Map.of("orderId", orderId,
                "items", List.of(Map.of("sku", "A", "qty", 1)), "warehouseId", "WH-1")));
        fulfillWorker.execute(fulTask);

        return orderId;
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
