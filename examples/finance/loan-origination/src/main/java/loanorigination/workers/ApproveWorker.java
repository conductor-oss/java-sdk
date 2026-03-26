package loanorigination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Approves the loan after underwriting.
 * Input: applicationId, underwritingDecision, approvedRate
 * Output: approved, approvalNumber
 */
public class ApproveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lnr_approve";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicationId = (String) task.getInputData().get("applicationId");
        if (applicationId == null) applicationId = "unknown";
        Object decision = task.getInputData().get("underwritingDecision");
        Object rate = task.getInputData().get("approvedRate");

        System.out.println("  [approve] Loan " + applicationId + ": " + decision + " at " + rate + "%");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", true);
        result.getOutputData().put("approvalNumber", "APR-LN-44201");
        return result;
    }
}
