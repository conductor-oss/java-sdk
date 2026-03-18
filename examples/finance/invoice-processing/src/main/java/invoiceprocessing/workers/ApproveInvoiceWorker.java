package invoiceprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Approves an invoice based on PO match status and amount thresholds.
 * Real approval logic with threshold-based routing.
 */
public class ApproveInvoiceWorker implements Worker {
    @Override public String getTaskDefName() { return "ivc_approve_invoice"; }

    @Override public TaskResult execute(Task task) {
        Object amountObj = task.getInputData().get("amount");
        Object poMatchedObj = task.getInputData().get("poMatched");

        double amount = 0;
        if (amountObj instanceof Number) amount = ((Number) amountObj).doubleValue();
        boolean poMatched = Boolean.TRUE.equals(poMatchedObj) || "true".equals(String.valueOf(poMatchedObj));

        // Real approval logic
        String approver;
        boolean approved;
        String reason;
        if (!poMatched) {
            approved = false;
            approver = "SYSTEM";
            reason = "PO mismatch - manual review required";
        } else if (amount > 50000) {
            approver = "CFO";
            approved = true;
            reason = "High-value invoice approved by CFO";
        } else if (amount > 10000) {
            approver = "VP-Finance";
            approved = true;
            reason = "Approved by VP-Finance";
        } else {
            approver = "AP-Manager";
            approved = true;
            reason = "Auto-approved within threshold";
        }

        System.out.println("  [approve] Invoice $" + String.format("%.2f", amount)
                + " - PO matched: " + poMatched + " - " + (approved ? "APPROVED" : "REJECTED")
                + " by " + approver);

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("approved", approved);
        r.getOutputData().put("approver", approver);
        r.getOutputData().put("reason", reason);
        return r;
    }
}
