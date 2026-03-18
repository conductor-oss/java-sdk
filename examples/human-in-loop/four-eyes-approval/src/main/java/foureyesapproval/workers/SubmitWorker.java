package foureyesapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Submits a request for dual (four-eyes) approval.
 * Real validation of request ID and determines required approval levels.
 */
public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "fep_submit"; }

    @Override public TaskResult execute(Task task) {
        Object rawId = task.getInputData().get("requestId");
        Object rawAmount = task.getInputData().get("amount");
        String requestId = (rawId instanceof String) ? (String) rawId : "unknown";

        double amount = 0;
        if (rawAmount instanceof Number) amount = ((Number) rawAmount).doubleValue();

        boolean valid = !requestId.equals("unknown");

        // Determine required approval tiers based on amount
        String tier1Role = "Peer Reviewer";
        String tier2Role = amount > 50000 ? "Director" : "Manager";

        System.out.println("  [fep_submit] Request " + requestId + " requires: "
                + tier1Role + " + " + tier2Role);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("submitted", valid);
        result.getOutputData().put("requestId", requestId);
        result.getOutputData().put("tier1Role", tier1Role);
        result.getOutputData().put("tier2Role", tier2Role);
        result.getOutputData().put("submittedAt", Instant.now().toString());
        return result;
    }
}
