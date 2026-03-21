package eventsplit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SplitEventWorkerTest {

    private final SplitEventWorker worker = new SplitEventWorker();

    @Test
    void taskDefName() {
        assertEquals("sp_split_event", worker.getTaskDefName());
    }

    @Test
    void splitsCompositeEventIntoThreeSubEvents() {
        Map<String, Object> event = Map.of(
                "order", Map.of("orderId", "ORD-700"),
                "customer", Map.of("id", "CUST-88"),
                "shipping", Map.of("method", "express"));
        Task task = taskWith(Map.of("event", event));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("subEventA"));
        assertNotNull(result.getOutputData().get("subEventB"));
        assertNotNull(result.getOutputData().get("subEventC"));
        assertEquals(3, result.getOutputData().get("subEventCount"));
    }

    @Test
    void subEventAContainsOrderDetails() {
        Map<String, Object> event = Map.of(
                "order", Map.of("orderId", "ORD-100", "items", 3),
                "customer", Map.of("id", "C1"),
                "shipping", Map.of("method", "std"));
        Task task = taskWith(Map.of("event", event));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> subEventA = (Map<String, Object>) result.getOutputData().get("subEventA");
        assertEquals("order_details", subEventA.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) subEventA.get("data");
        assertEquals("ORD-100", data.get("orderId"));
    }

    @Test
    void subEventBContainsCustomerInfo() {
        Map<String, Object> event = Map.of(
                "order", Map.of("orderId", "ORD-200"),
                "customer", Map.of("id", "CUST-55", "name", "Bob"),
                "shipping", Map.of("method", "express"));
        Task task = taskWith(Map.of("event", event));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> subEventB = (Map<String, Object>) result.getOutputData().get("subEventB");
        assertEquals("customer_info", subEventB.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) subEventB.get("data");
        assertEquals("CUST-55", data.get("id"));
        assertEquals("Bob", data.get("name"));
    }

    @Test
    void subEventCContainsShippingInfo() {
        Map<String, Object> event = Map.of(
                "order", Map.of("orderId", "ORD-300"),
                "customer", Map.of("id", "C3"),
                "shipping", Map.of("method", "overnight", "address", "123 Main St"));
        Task task = taskWith(Map.of("event", event));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> subEventC = (Map<String, Object>) result.getOutputData().get("subEventC");
        assertEquals("shipping_info", subEventC.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) subEventC.get("data");
        assertEquals("overnight", data.get("method"));
        assertEquals("123 Main St", data.get("address"));
    }

    @Test
    void handlesNullEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("event", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("subEventCount"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("subEventA"));
        assertNotNull(result.getOutputData().get("subEventB"));
        assertNotNull(result.getOutputData().get("subEventC"));
    }

    @Test
    void subEventCountIsAlwaysThree() {
        Task task = taskWith(Map.of("event", Map.of("order", Map.of())));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("subEventCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
