package eventdrivenmicroservices.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShippingServiceWorkerTest {

    private final ShippingServiceWorker worker = new ShippingServiceWorker();

    @Test
    void taskDefName() {
        assertEquals("dm_shipping_service", worker.getTaskDefName());
    }

    @Test
    void createsShipment() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-DM-1001",
                "address", "789 Elm Street, San Jose, CA 95112",
                "items", List.of(Map.of("name", "Laptop Stand"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SHIP-12345", result.getOutputData().get("trackingNumber"));
        assertEquals("3-5 business days", result.getOutputData().get("estimatedDelivery"));
        assertEquals("label_created", result.getOutputData().get("status"));
    }

    @Test
    void outputContainsTrackingNumber() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-100",
                "address", "123 Main St",
                "items", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals("SHIP-12345", result.getOutputData().get("trackingNumber"));
    }

    @Test
    void outputContainsEstimatedDelivery() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-200",
                "address", "456 Oak Ave",
                "items", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals("3-5 business days", result.getOutputData().get("estimatedDelivery"));
    }

    @Test
    void outputStatusIsLabelCreated() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-300",
                "address", "789 Pine Rd",
                "items", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals("label_created", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullAddress() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-400");
        input.put("address", null);
        input.put("items", List.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SHIP-12345", result.getOutputData().get("trackingNumber"));
    }

    @Test
    void handlesNullItems() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-500");
        input.put("address", "555 Birch Ln");
        input.put("items", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SHIP-12345", result.getOutputData().get("trackingNumber"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SHIP-12345", result.getOutputData().get("trackingNumber"));
        assertEquals("3-5 business days", result.getOutputData().get("estimatedDelivery"));
        assertEquals("label_created", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
