package stripeintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends a receipt email.
 * Input: email, chargeId, amount, currency
 * Output: sent, sentAt
 *
 * This worker is always deterministic.— receipt sending is handled outside the
 * Stripe API (e.g., via SendGrid or another email service).
 */
public class SendReceiptWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "stp_send_receipt";
    }

    @Override
    public TaskResult execute(Task task) {
        String email = (String) task.getInputData().get("email");
        String chargeId = (String) task.getInputData().get("chargeId");
        System.out.println("  [receipt] Sent receipt for " + chargeId + " to " + email);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);
        result.getOutputData().put("sentAt", java.time.Instant.now().toString());
        return result;
    }
}
