package paymentreconciliation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Resolves identified mismatches/discrepancies.
 * Input: batchId, discrepancies
 * Output: resolvedCount, unresolvedCount, resolutions, pendingReview
 */
public class ResolveMismatchesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prc_resolve_mismatches";
    }

    @Override
    public TaskResult execute(Task task) {
        Object discrepancies = task.getInputData().get("discrepancies");
        int count = 0;
        if (discrepancies instanceof List) {
            count = ((List<?>) discrepancies).size();
        }

        System.out.println("  [resolve] Resolving " + count + " discrepancies");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("resolvedCount", 1);
        result.getOutputData().put("unresolvedCount", 1);
        result.getOutputData().put("resolutions", List.of(
                Map.of("txnId", "TXN-002", "action", "adjusted", "adjustedAmount", 830.75)
        ));
        result.getOutputData().put("pendingReview", List.of(
                Map.of("txnId", "TXN-001", "reason", "Requires manual investigation")
        ));
        return result;
    }
}
