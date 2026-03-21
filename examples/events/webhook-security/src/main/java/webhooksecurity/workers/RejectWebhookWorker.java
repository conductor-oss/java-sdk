package webhooksecurity.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Rejects a webhook with an invalid signature.
 * Input: reason, provided
 * Output: rejected (true), reason
 */
public class RejectWebhookWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ws_reject_webhook";
    }

    @Override
    public TaskResult execute(Task task) {
        String reason = (String) task.getInputData().get("reason");
        String provided = (String) task.getInputData().get("provided");

        if (reason == null) {
            reason = "unknown";
        }
        if (provided == null) {
            provided = "";
        }

        System.out.println("  [ws_reject_webhook] Rejecting webhook: " + reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rejected", true);
        result.getOutputData().put("reason", reason);
        return result;
    }
}
