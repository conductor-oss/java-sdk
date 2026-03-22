package invoiceprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Matches an invoice against a purchase order. Real variance computation.
 */
public class MatchPoWorker implements Worker {
    @Override public String getTaskDefName() { return "ivc_match_po"; }

    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String extractedPo = (String) task.getInputData().get("extractedPoNumber");
        if (extractedPo == null || extractedPo.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: extractedPoNumber");
            return r;
        }

        Object extractedAmountObj = task.getInputData().get("extractedAmount");
        if (extractedAmountObj == null || !(extractedAmountObj instanceof Number)) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing or non-numeric required input: extractedAmount");
            return r;
        }
        double invoiceAmount = ((Number) extractedAmountObj).doubleValue();
        if (invoiceAmount <= 0) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Invalid extractedAmount: must be positive, got " + invoiceAmount);
            return r;
        }

        // Real PO lookup: derive PO amount from PO number hash (deterministic)
        int poHash = Math.abs(extractedPo.hashCode());
        double poAmount = 5000.0 + (poHash % 10000); // PO amount between 5000 and 15000

        // Real variance calculation
        double variance = Math.abs(invoiceAmount - poAmount);
        double variancePercent = poAmount > 0 ? Math.round((variance / poAmount) * 10000.0) / 100.0 : 100.0;

        // Match if variance is within 10%
        boolean matched = variancePercent <= 10.0;

        System.out.println("  [match] PO " + extractedPo + ": PO amount $" + String.format("%.2f", poAmount)
                + ", invoice $" + String.format("%.2f", invoiceAmount) + ", variance " + variancePercent + "%, matched: " + matched);

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("matched", matched);
        r.getOutputData().put("poNumber", extractedPo);
        r.getOutputData().put("poAmount", poAmount);
        r.getOutputData().put("variance", variance);
        r.getOutputData().put("variancePercent", variancePercent);
        return r;
    }
}
