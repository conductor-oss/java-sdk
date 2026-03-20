package idempotentworkers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotent email worker — deduplication with a Set.
 *
 * Uses orderId:email as the dedup key. If a notification has already been sent
 * for this combination, returns {sent: true, duplicate: true} without
 * re-sending.
 */
public class SendEmailWorker implements Worker {

    private final Set<String> sentEmails = ConcurrentHashMap.newKeySet();

    @Override
    public String getTaskDefName() {
        return "idem_send_email";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        String email = (String) task.getInputData().get("email");

        String dedupKey = orderId + ":" + email;

        TaskResult result = new TaskResult(task);

        if (sentEmails.contains(dedupKey)) {
            System.out.println("  [idem_send_email] Duplicate email detected for " + dedupKey + " — skipping");
            result.getOutputData().put("sent", true);
            result.getOutputData().put("duplicate", true);
            result.getOutputData().put("orderId", orderId);
            result.getOutputData().put("email", email);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }

        // Send the email
        System.out.println("  [idem_send_email] Sending confirmation email to " + email + " for order " + orderId);
        sentEmails.add(dedupKey);

        result.getOutputData().put("sent", true);
        result.getOutputData().put("duplicate", false);
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("email", email);
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

    /**
     * Clears the sent-emails set. Useful for testing.
     */
    public void clearSentEmails() {
        sentEmails.clear();
    }
}
