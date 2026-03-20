package loanorigination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Runs a credit check for the loan applicant.
 * Input: applicantId, loanAmount
 * Output: creditScore, bureau, dti, delinquencies, openAccounts
 */
public class CreditCheckWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lnr_credit_check";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicantId = (String) task.getInputData().get("applicantId");
        if (applicantId == null) applicantId = "unknown";

        System.out.println("  [credit] Running credit check for applicant " + applicantId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("creditScore", 742);
        result.getOutputData().put("bureau", "Experian");
        result.getOutputData().put("dti", 28);
        result.getOutputData().put("delinquencies", 0);
        result.getOutputData().put("openAccounts", 5);
        return result;
    }
}
