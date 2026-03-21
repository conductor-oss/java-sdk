package paymentreconciliation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Matches transactions against records for reconciliation.
 * Input: batchId, accountId, periodStart, periodEnd
 * Output: matchedCount, unmatchedCount, totalAmount, unmatchedItems
 */
public class MatchTransactionsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prc_match_transactions";
    }

    @Override
    public TaskResult execute(Task task) {
        String batchId = (String) task.getInputData().get("batchId");
        if (batchId == null) batchId = "UNKNOWN";

        System.out.println("  [match] Matching transactions for batch " + batchId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("matchedCount", 145);
        result.getOutputData().put("unmatchedCount", 8);
        result.getOutputData().put("totalAmount", 283210.50);
        result.getOutputData().put("unmatchedItems", List.of(
                Map.of("txnId", "TXN-001", "amount", 1250.00, "reason", "missing_record"),
                Map.of("txnId", "TXN-002", "amount", 830.75, "reason", "amount_mismatch")
        ));
        return result;
    }
}
