package webhookretry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Attempts to deliver the webhook payload to the target URL.
 * perform  transient failures for early attempts and success on attempt >= 3.
 * Input: webhookUrl, payload, attempt
 * Output: statusCode, attempt, backoffMs
 */
public class AttemptDeliveryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wr_attempt_delivery";
    }

    @Override
    public TaskResult execute(Task task) {
        Object attemptRaw = task.getInputData().get("attempt");
        int attempt = 1;
        if (attemptRaw instanceof Number) {
            attempt = ((Number) attemptRaw).intValue();
        }

        int statusCode;
        int backoffMs;
        if (attempt < 3) {
            statusCode = 503;
            backoffMs = attempt * 1000;
        } else {
            statusCode = 200;
            backoffMs = 0;
        }

        System.out.println("  [wr_attempt_delivery] Attempt " + attempt + " -> HTTP " + statusCode);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("statusCode", statusCode);
        result.getOutputData().put("attempt", attempt);
        result.getOutputData().put("backoffMs", backoffMs);
        return result;
    }
}
