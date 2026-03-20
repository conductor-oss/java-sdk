package eventdrivenmicroservices.workers;

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
        assertEquals("dm_order_service", worker.getTaskDefName());
    }

    @Test
    void createsOrderWithItems() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-100",
                "items", List.of(
                        Map.of("name", "Laptop Stand", "price", 49.99, "qty", 1),
                        Map.of("name", "USB-C Hub", "price", 34.99, "qty", 2),
                        Map.of("name", "Monitor Light Bar", "price", 79.99, "qty", 1))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-DM-1001", result.getOutputData().get("orderId"));
        assertEquals("created", result.getOutputData().get("status"));
    }

    @Test
    void calculatesTotalAmount() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-200",
                "items", List.of(
                        Map.of("name", "Laptop Stand", "price", 49.99, "qty", 1),
                        Map.of("name", "USB-C Hub", "price", 34.99, "qty", 2),
                        Map.of("name", "Monitor Light Bar", "price", 79.99, "qty", 1))));
        TaskResult result = worker.execute(task);

        // 49.99*1 + 34.99*2 + 79.99*1 = 199.96
        double total = ((Number) result.getOutputData().get("totalAmount")).doubleValue();
        assertEquals(199.96, total, 0.01);
    }

    @Test
    void outputContainsItems() {
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Widget", "price", 10.0, "qty", 3));
        Task task = taskWith(Map.of("customerId", "CUST-300", "items", items));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outputItems =
                (List<Map<String, Object>>) result.getOutputData().get("items");
        assertEquals(1, outputItems.size());
        assertEquals("Widget", outputItems.get(0).get("name"));
    }

    @Test
    void singleItemOrder() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-400",
                "items", List.of(Map.of("name", "Book", "price", 15.0, "qty", 1))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        double total = ((Number) result.getOutputData().get("totalAmount")).doubleValue();
        assertEquals(15.0, total, 0.01);
    }

    @Test
    void handlesEmptyItemsList() {
        Task task = taskWith(Map.of("customerId", "CUST-500", "items", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        double total = ((Number) result.getOutputData().get("totalAmount")).doubleValue();
        assertEquals(0.0, total, 0.01);
    }

    @Test
    void handlesNullItems() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", "CUST-600");
        input.put("items", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-DM-1001", result.getOutputData().get("orderId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-DM-1001", result.getOutputData().get("orderId"));
        assertEquals("created", result.getOutputData().get("status"));
    }

    @Test
    void defaultQtyIsOne() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-700",
                "items", List.of(Map.of("name", "Pen", "price", 5.0))));
        TaskResult result = worker.execute(task);

        double total = ((Number) result.getOutputData().get("totalAmount")).doubleValue();
        assertEquals(5.0, total, 0.01);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
