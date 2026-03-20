package eventdrivenmicroservices.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceWorkerTest {

    private final NotificationServiceWorker worker = new NotificationServiceWorker();

    @Test
    void taskDefName() {
        assertEquals("dm_notification_service", worker.getTaskDefName());
    }

    @Test
    void sendsNotification() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-DM-100",
                "orderId", "ORD-DM-1001",
                "trackingNumber", "SHIP-12345",
                "transactionId", "TXN-fixed-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
        assertEquals("CUST-DM-100", result.getOutputData().get("customerId"));
    }

    @Test
    void outputContainsChannels() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-100",
                "orderId", "ORD-100",
                "trackingNumber", "SHIP-100",
                "transactionId", "TXN-100"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> channels = (List<String>) result.getOutputData().get("channels");
        assertEquals(List.of("email", "push"), channels);
    }

    @Test
    void outputContainsSentTrue() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-200",
                "orderId", "ORD-200",
                "trackingNumber", "SHIP-200",
                "transactionId", "TXN-200"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("sent"));
    }

    @Test
    void outputContainsCustomerId() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-300",
                "orderId", "ORD-300",
                "trackingNumber", "SHIP-300",
                "transactionId", "TXN-300"));
        TaskResult result = worker.execute(task);

        assertEquals("CUST-300", result.getOutputData().get("customerId"));
    }

    @Test
    void handlesNullCustomerId() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", null);
        input.put("orderId", "ORD-400");
        input.put("trackingNumber", "SHIP-400");
        input.put("transactionId", "TXN-400");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("customerId"));
    }

    @Test
    void handlesNullTrackingNumber() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", "CUST-500");
        input.put("orderId", "ORD-500");
        input.put("trackingNumber", null);
        input.put("transactionId", "TXN-500");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
        assertEquals("unknown", result.getOutputData().get("customerId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
