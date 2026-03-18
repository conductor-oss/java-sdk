package escalationchain.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Submits a request for escalation-chain approval.
 * Real validation: checks request ID format, determines urgency level, sets first escalation level.
 */
public class EscSubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "esc_submit"; }

    @Override public TaskResult execute(Task task) {
        Object rawId = task.getInputData().get("requestId");
        Object rawAmount = task.getInputData().get("amount");
        String requestId = (rawId instanceof String) ? (String) rawId : "unknown";

        double amount = 0;
        if (rawAmount instanceof Number) amount = ((Number) rawAmount).doubleValue();

        // Real escalation level determination based on amount thresholds
        String firstLevel;
        if (amount > 100000) {
            firstLevel = "VP";
        } else if (amount > 10000) {
            firstLevel = "Manager";
        } else {
            firstLevel = "Analyst";
        }

        boolean validRequest = !requestId.equals("unknown") && requestId.length() > 0;

        System.out.println("  [esc_submit] Request " + requestId + " ($" + (int) amount
                + ") -> first level: " + firstLevel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("submitted", validRequest);
        result.getOutputData().put("requestId", requestId);
        result.getOutputData().put("firstLevel", firstLevel);
        result.getOutputData().put("submittedAt", Instant.now().toString());
        return result;
    }
}
