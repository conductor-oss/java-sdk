package contractlifecycle.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Approves a contract. Real threshold-based approval routing.
 */
public class ApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "clf_approve"; }

    @Override public TaskResult execute(Task task) {
        Object amountObj = task.getInputData().get("amount");
        Object reviewPassedObj = task.getInputData().get("reviewPassed");
        double amount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : 0;
        boolean reviewPassed = Boolean.TRUE.equals(reviewPassedObj);

        String approver;
        if (amount > 100000) approver = "CEO";
        else if (amount > 50000) approver = "VP-Procurement";
        else approver = "Procurement Manager";

        boolean approved = reviewPassed;

        System.out.println("  [approve] " + (approved ? "Approved" : "Rejected") + " by " + approver);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("contractState", approved ? "APPROVED" : "REJECTED");
        result.getOutputData().put("approved", approved);
        result.getOutputData().put("approver", approver);
        return result;
    }
}
