package procurementworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Approves or rejects a requisition based on budget thresholds.
 * Real multi-level approval logic.
 */
public class ApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "prw_approve"; }

    @Override public TaskResult execute(Task task) {
        Object costObj = task.getInputData().get("estimatedCost");
        Object budgetObj = task.getInputData().get("budget");
        double estimatedCost = costObj instanceof Number ? ((Number) costObj).doubleValue() : 0;
        double budget = budgetObj instanceof Number ? ((Number) budgetObj).doubleValue() : 0;

        boolean withinBudget = estimatedCost <= budget;
        String approver;
        if (estimatedCost > 50000) approver = "CFO";
        else if (estimatedCost > 10000) approver = "VP-Operations";
        else approver = "mgr-finance";

        double approvedAmount = withinBudget ? estimatedCost : 0;

        System.out.println("  [approve] " + (withinBudget ? "Approved" : "Rejected")
                + " by " + approver + " - cost $" + (int) estimatedCost + " vs budget $" + (int) budget);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", withinBudget);
        result.getOutputData().put("approver", approver);
        result.getOutputData().put("approvedAmount", approvedAmount);
        return result;
    }
}
