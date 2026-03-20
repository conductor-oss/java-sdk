package loanorigination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives a loan application and records intake details.
 * Input: applicationId, applicantId, loanAmount, loanType
 * Output: received, employment, receivedAt
 */
public class ApplicationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lnr_application";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicationId = (String) task.getInputData().get("applicationId");
        if (applicationId == null) applicationId = "unknown";
        Object loanAmount = task.getInputData().get("loanAmount");
        String loanType = (String) task.getInputData().get("loanType");
        if (loanType == null) loanType = "personal";

        System.out.println("  [application] Loan app " + applicationId + ": $" + loanAmount + " " + loanType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("received", true);
        result.getOutputData().put("employment", Map.of(
                "employer", "TechCorp",
                "years", 5,
                "income", 120000));
        result.getOutputData().put("receivedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
