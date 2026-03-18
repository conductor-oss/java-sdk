package understandingworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends an order confirmation email.
 *
 * Input:  customerEmail (String), total (double), orderId (String)
 * Output: emailSent (boolean), recipient (String)
 */
public class SendConfirmationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "send_confirmation";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerEmail = (String) task.getInputData().get("customerEmail");
        Object total = task.getInputData().get("total");
        String orderId = (String) task.getInputData().get("orderId");

        System.out.println("  [send_confirmation] Sending confirmation to "
                + customerEmail + " for order " + orderId + " ($" + total + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("emailSent", true);
        result.getOutputData().put("recipient", customerEmail != null ? customerEmail : "unknown");
        return result;
    }
}
