package eventchoreography.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends a notification to the customer.
 * Input: orderId, customerId, event
 * Output: notificationStatus ("sent"), channel ("email")
 */
public class NotificationServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ch_notification_service";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        String customerId = (String) task.getInputData().get("customerId");
        String event = (String) task.getInputData().get("event");
        if (event == null) {
            event = "unknown";
        }

        System.out.println("  [ch_notification_service] Sending " + event + " notification to customer " + customerId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notificationStatus", "sent");
        result.getOutputData().put("channel", "email");
        return result;
    }
}
