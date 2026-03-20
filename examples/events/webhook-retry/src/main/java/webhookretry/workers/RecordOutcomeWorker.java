package webhookretry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Records the final outcome of the webhook delivery process.
 * Input: totalAttempts, webhookUrl
 * Output: outcome, totalAttempts
 */
public class RecordOutcomeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wr_record_outcome";
    }

    @Override
    public TaskResult execute(Task task) {
        Object totalAttemptsRaw = task.getInputData().get("totalAttempts");
        int totalAttempts = 1;
        if (totalAttemptsRaw instanceof Number) {
            totalAttempts = ((Number) totalAttemptsRaw).intValue();
        }

        String webhookUrl = (String) task.getInputData().get("webhookUrl");
        if (webhookUrl == null) {
            webhookUrl = "unknown";
        }

        System.out.println("  [wr_record_outcome] Delivered to " + webhookUrl + " after " + totalAttempts + " attempts");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("outcome", "delivered after retries");
        result.getOutputData().put("totalAttempts", totalAttempts);
        return result;
    }
}
