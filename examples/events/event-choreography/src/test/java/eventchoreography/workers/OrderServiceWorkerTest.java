package eventchoreography.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceWorkerTest {

    private final OrderServiceWorker worker = new OrderServiceWorker();

    @Test
    void taskDefName() {
        assertEquals("ch_order_service", worker.getTaskDefName());
    }

    @Test
    void processesOrderWithItems() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-100",
                "items", List.of(Map.of("sku", "ITEM-A", "price", 29.99), Map.of("sku", "ITEM-B", "price", 49.99)),
                "customerId", "CUST-42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-100", result.getOutputData().get("orderId"));
        assertEquals(79.98, result.getOutputData().get("totalAmount"));
        assertEquals("created", result.getOutputData().get("status"));
    }

    @Test
    void outputContainsItems() {
        List<Map<String, Object>> items = List.of(
                Map.of("sku", "ITEM-A", "price", 29.99),
                Map.of("sku", "ITEM-B", "price", 49.99));
        Task task = taskWith(Map.of("orderId", "ORD-200", "items", items, "customerId", "CUST-10"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("items"));
    }

    @Test
    void outputContainsTotalAmount() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-300",
                "items", List.of(Map.of("sku", "X", "price", 10.0)),
                "customerId", "CUST-5"));
        TaskResult result = worker.execute(task);

        assertEquals(79.98, result.getOutputData().get("totalAmount"));
    }

    @Test
    void statusIsCreated() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-400",
                "items", List.of(),
                "customerId", "CUST-1"));
        TaskResult result = worker.execute(task);

        assertEquals("created", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullOrderId() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", null);
        input.put("items", List.of());
        input.put("customerId", "CUST-1");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-UNKNOWN", result.getOutputData().get("orderId"));
    }

    @Test
    void handlesNullItems() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-500");
        input.put("items", null);
        input.put("customerId", "CUST-2");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("items"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-UNKNOWN", result.getOutputData().get("orderId"));
        assertEquals(79.98, result.getOutputData().get("totalAmount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
