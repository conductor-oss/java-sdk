package paymentreconciliation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Identifies discrepancies from unmatched transactions.
 * Input: batchId, matchedCount, unmatchedCount, unmatchedItems
 * Output: discrepancies, totalDiscrepancyAmount
 */
public class IdentifyDiscrepanciesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prc_identify_discrepancies";
    }

    @Override
    public TaskResult execute(Task task) {
        Object unmatchedCount = task.getInputData().get("unmatchedCount");
        if (unmatchedCount == null) unmatchedCount = 0;

        System.out.println("  [discrepancy] Found " + unmatchedCount + " unmatched items");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("discrepancies", List.of(
                Map.of("txnId", "TXN-001", "type", "missing_record", "amount", 1250.00, "severity", "high"),
                Map.of("txnId", "TXN-002", "type", "amount_mismatch", "amount", 830.75, "severity", "medium")
        ));
        result.getOutputData().put("totalDiscrepancyAmount", 2080.75);
        return result;
    }
}
