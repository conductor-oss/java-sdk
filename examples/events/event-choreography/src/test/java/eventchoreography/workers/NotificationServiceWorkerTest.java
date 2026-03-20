package eventchoreography.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceWorkerTest {

    private final NotificationServiceWorker worker = new NotificationServiceWorker();

    @Test
    void taskDefName() {
        assertEquals("ch_notification_service", worker.getTaskDefName());
    }

    @Test
    void sendsNotification() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-100",
                "customerId", "CUST-42",
                "event", "order.confirmed"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("sent", result.getOutputData().get("notificationStatus"));
        assertEquals("email", result.getOutputData().get("channel"));
    }

    @Test
    void outputContainsNotificationStatus() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-200",
                "customerId", "CUST-10",
                "event", "order.shipped"));
        TaskResult result = worker.execute(task);

        assertEquals("sent", result.getOutputData().get("notificationStatus"));
    }

    @Test
    void outputContainsChannel() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-300",
                "customerId", "CUST-5",
                "event", "payment.received"));
        TaskResult result = worker.execute(task);

        assertEquals("email", result.getOutputData().get("channel"));
    }

    @Test
    void handlesNullEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-400");
        input.put("customerId", "CUST-1");
        input.put("event", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("sent", result.getOutputData().get("notificationStatus"));
    }

    @Test
    void handlesNullCustomerId() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-500");
        input.put("customerId", null);
        input.put("event", "order.confirmed");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("email", result.getOutputData().get("channel"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("sent", result.getOutputData().get("notificationStatus"));
        assertEquals("email", result.getOutputData().get("channel"));
    }

    @Test
    void channelIsAlwaysEmail() {
        Task task1 = taskWith(Map.of("orderId", "ORD-A", "customerId", "CUST-1", "event", "order.confirmed"));
        Task task2 = taskWith(Map.of("orderId", "ORD-B", "customerId", "CUST-2", "event", "order.shipped"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("channel"),
                     result2.getOutputData().get("channel"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
