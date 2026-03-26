package loanorigination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Disburses the loan funds to the applicant.
 * Input: applicationId, applicantId, loanAmount, approvedRate
 * Output: funded, disbursementId, fundedAt, firstPaymentDate
 */
public class FundWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lnr_fund";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicantId = (String) task.getInputData().get("applicantId");
        if (applicantId == null) applicantId = "unknown";
        Object loanAmount = task.getInputData().get("loanAmount");

        System.out.println("  [fund] Disbursing $" + loanAmount + " to applicant " + applicantId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("funded", true);
        result.getOutputData().put("disbursementId", "DISB-88201");
        result.getOutputData().put("fundedAt", "2026-01-15T10:05:00Z");
        result.getOutputData().put("firstPaymentDate", "2024-05-01");
        return result;
    }
}
