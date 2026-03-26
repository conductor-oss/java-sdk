package ordermanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ValidateOrderWorkerTest {

    private CreateOrderWorker createWorker;
    private ValidateOrderWorker validateWorker;

    @BeforeEach
    void setUp() {
        OrderStore.reset();
        createWorker = new CreateOrderWorker();
        validateWorker = new ValidateOrderWorker();
    }

    @Test
    void taskDefName() { assertEquals("ord_validate", validateWorker.getTaskDefName()); }

    @Test
    void validatesOrder() {
        String orderId = createOrder("cust-1", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1)));

        Task task = taskWith(Map.of("orderId", orderId, "items",
                List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1))));
        TaskResult r = validateWorker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("valid"));
        assertEquals(true, r.getOutputData().get("allInStock"));
    }

    @Test
    void transitionsToConfirmed() {
        String orderId = createOrder("cust-2", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1)));

        Task task = taskWith(Map.of("orderId", orderId, "items",
                List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1))));
        validateWorker.execute(task);

        assertEquals("CONFIRMED", OrderStore.getStatus(orderId));
    }

    @Test
    void includesStockCheck() {
        String orderId = createOrder("cust-3", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 2)));

        Task task = taskWith(Map.of("orderId", orderId, "items",
                List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 2))));
        TaskResult r = validateWorker.execute(task);

        assertNotNull(r.getOutputData().get("stockCheck"));
    }

    @Test
    void tracksPreviousStatus() {
        String orderId = createOrder("cust-4", List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1)));

        Task task = taskWith(Map.of("orderId", orderId, "items",
                List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1))));
        TaskResult r = validateWorker.execute(task);

        assertEquals("CREATED", r.getOutputData().get("previousStatus"));
        assertEquals("CONFIRMED", r.getOutputData().get("status"));
    }

    // ---- Failure path ---------------------------------------------------

    @Test
    void failsWithTerminalErrorOnMissingOrderId() {
        Task task = taskWith(Map.of("items",
                List.of(Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1))));
        TaskResult r = validateWorker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("orderId"));
    }

    @Test
    void failsWithTerminalErrorOnMissingItems() {
        Task task = taskWith(Map.of("orderId", "ORD-123"));
        TaskResult r = validateWorker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("items"));
    }

    @Test
    void failsWithTerminalErrorOnEmptyItems() {
        Task task = taskWith(Map.of("orderId", "ORD-123", "items", List.of()));
        TaskResult r = validateWorker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    private String createOrder(String customerId, List<Map<String, Object>> items) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("customerId", customerId, "items", items)));
        TaskResult r = createWorker.execute(task);
        return r.getOutputData().get("orderId").toString();
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
