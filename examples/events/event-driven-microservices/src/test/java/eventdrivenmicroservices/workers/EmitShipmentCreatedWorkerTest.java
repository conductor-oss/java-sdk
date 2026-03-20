package eventdrivenmicroservices.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmitShipmentCreatedWorkerTest {

    private final EmitShipmentCreatedWorker worker = new EmitShipmentCreatedWorker();

    @Test
    void taskDefName() {
        assertEquals("dm_emit_shipment_created", worker.getTaskDefName());
    }

    @Test
    void publishesShipmentCreatedEvent() {
        Task task = taskWith(Map.of(
                "eventType", "shipment.created",
                "orderId", "ORD-DM-1001",
                "trackingNumber", "SHIP-12345"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
        assertEquals("shipment.created", result.getOutputData().get("eventType"));
    }

    @Test
    void outputContainsPublishedTrue() {
        Task task = taskWith(Map.of(
                "eventType", "shipment.created",
                "orderId", "ORD-100",
                "trackingNumber", "SHIP-100"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("published"));
    }

    @Test
    void outputContainsEventType() {
        Task task = taskWith(Map.of(
                "eventType", "shipment.created",
                "orderId", "ORD-200",
                "trackingNumber", "SHIP-200"));
        TaskResult result = worker.execute(task);

        assertEquals("shipment.created", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesCustomEventType() {
        Task task = taskWith(Map.of(
                "eventType", "shipment.updated",
                "orderId", "ORD-300",
                "trackingNumber", "SHIP-300"));
        TaskResult result = worker.execute(task);

        assertEquals("shipment.updated", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", null);
        input.put("orderId", "ORD-400");
        input.put("trackingNumber", "SHIP-400");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("shipment.created", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullTrackingNumber() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventType", "shipment.created");
        input.put("orderId", "ORD-500");
        input.put("trackingNumber", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
        assertEquals("shipment.created", result.getOutputData().get("eventType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
