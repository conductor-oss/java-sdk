package contractlifecycle.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Drafts a new contract. Real contract state machine - DRAFT state.
 */
public class DraftWorker implements Worker {
    @Override public String getTaskDefName() { return "clf_draft"; }

    @Override public TaskResult execute(Task task) {
        String vendorName = (String) task.getInputData().get("vendorName");
        Object amountObj = task.getInputData().get("amount");
        if (vendorName == null) vendorName = "Unknown Vendor";
        double amount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : 0;

        String contractId = "CTR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [draft] Contract " + contractId + " for " + vendorName + " ($" + (int) amount + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("contractId", contractId);
        result.getOutputData().put("contractState", "DRAFT");
        result.getOutputData().put("draftedAt", Instant.now().toString());
        return result;
    }
}
