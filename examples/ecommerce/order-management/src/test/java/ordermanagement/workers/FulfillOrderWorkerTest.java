package ordermanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FulfillOrderWorkerTest {

    private CreateOrderWorker createWorker;
    private ValidateOrderWorker validateWorker;
    private FulfillOrderWorker fulfillWorker;

    @BeforeEach
    void setUp() {
        OrderStore.reset();
        createWorker = new CreateOrderWorker();
        validateWorker = new ValidateOrderWorker();
        fulfillWorker = new FulfillOrderWorker();
    }

    @Test
    void taskDefName() { assertEquals("ord_fulfill", fulfillWorker.getTaskDefName()); }

    @Test
    void returnsFulfillmentId() {
        String orderId = createAndConfirmOrder();

        Task task = taskWith(Map.of("orderId", orderId, "items",
                List.of(Map.of("sku", "A", "qty", 1)), "warehouseId", "WH-1"));
        TaskResult r = fulfillWorker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("fulfillmentId").toString().startsWith("FUL-"));
    }

    @Test
    void transitionsToProcessing() {
        String orderId = createAndConfirmOrder();

        Task task = taskWith(Map.of("orderId", orderId, "items",
                List.of(Map.of("sku", "A", "qty", 1)), "warehouseId", "WH-1"));
        fulfillWorker.execute(task);

        assertEquals("PROCESSING", OrderStore.getStatus(orderId));
    }

    @Test
    void fulfillmentIdIsUnique() {
        String orderId1 = createAndConfirmOrder();
        String orderId2 = createAndConfirmOrder();

        Task t1 = taskWith(Map.of("orderId", orderId1, "items", List.of(), "warehouseId", "WH-1"));
        Task t2 = taskWith(Map.of("orderId", orderId2, "items", List.of(), "warehouseId", "WH-1"));
        assertNotEquals(fulfillWorker.execute(t1).getOutputData().get("fulfillmentId"),
                fulfillWorker.execute(t2).getOutputData().get("fulfillmentId"));
    }

    @Test
    void includesPackedAt() {
        String orderId = createAndConfirmOrder();

        Task task = taskWith(Map.of("orderId", orderId, "items",
                List.of(Map.of("sku", "A", "qty", 1)), "warehouseId", "WH-1"));
        TaskResult r = fulfillWorker.execute(task);
        assertNotNull(r.getOutputData().get("packedAt"));
    }

    @Test
    void tracksPreviousStatus() {
        String orderId = createAndConfirmOrder();

        Task task = taskWith(Map.of("orderId", orderId, "items",
                List.of(Map.of("sku", "A", "qty", 1)), "warehouseId", "WH-1"));
        TaskResult r = fulfillWorker.execute(task);
        assertEquals("CONFIRMED", r.getOutputData().get("previousStatus"));
        assertEquals("PROCESSING", r.getOutputData().get("status"));
    }

    private String createAndConfirmOrder() {
        // Create
        Task createTask = new Task();
        createTask.setStatus(Task.Status.IN_PROGRESS);
        createTask.setInputData(new HashMap<>(Map.of("customerId", "cust-" + System.nanoTime(),
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1)))));
        TaskResult createResult = createWorker.execute(createTask);
        String orderId = createResult.getOutputData().get("orderId").toString();

        // Validate (transitions to CONFIRMED)
        Task valTask = new Task();
        valTask.setStatus(Task.Status.IN_PROGRESS);
        valTask.setInputData(new HashMap<>(Map.of("orderId", orderId,
                "items", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1)))));
        validateWorker.execute(valTask);

        return orderId;
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
