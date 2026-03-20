package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Sends notifications to the customer about their order.
 * Input: customerId, orderId, trackingNumber, transactionId
 * Output: sent (true), channels (["email","push"]), customerId
 */
public class NotificationServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_notification_service";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        if (customerId == null) {
            customerId = "unknown";
        }

        String orderId = task.getInputData().get("orderId") != null
                ? String.valueOf(task.getInputData().get("orderId"))
                : "unknown";

        String trackingNumber = task.getInputData().get("trackingNumber") != null
                ? String.valueOf(task.getInputData().get("trackingNumber"))
                : "unknown";

        System.out.println("  [notification-svc] Sent order confirmation to customer " + customerId);
        System.out.println("    Order: " + orderId + ", Tracking: " + trackingNumber);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);
        result.getOutputData().put("channels", List.of("email", "push"));
        result.getOutputData().put("customerId", customerId);
        return result;
    }
}
