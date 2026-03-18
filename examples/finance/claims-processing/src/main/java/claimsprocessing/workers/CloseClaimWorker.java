package claimsprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Closes a claim after settlement.
 * Real logic: generates satisfaction survey, records closure date.
 */
public class CloseClaimWorker implements Worker {

    @Override
    public String getTaskDefName() { return "clp_close_claim"; }

    @Override
    public TaskResult execute(Task task) {
        Object settledAmount = task.getInputData().get("settledAmount");
        String paymentMethod = (String) task.getInputData().get("paymentMethod");
        String claimId = (String) task.getInputData().get("claimId");
        if (paymentMethod == null) paymentMethod = "unknown";
        if (claimId == null) claimId = "UNKNOWN";

        long amount = 0;
        if (settledAmount instanceof Number) amount = ((Number) settledAmount).longValue();

        String closedDate = LocalDate.now().toString();
        String surveyId = "SRV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [close] Claim " + claimId + " closed - settled $" + amount
                + " via " + paymentMethod + " (survey: " + surveyId + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("claimStatus", "closed");
        result.getOutputData().put("closedDate", closedDate);
        result.getOutputData().put("satisfactionSurveyId", surveyId);
        result.getOutputData().put("totalPaid", amount);
        return result;
    }
}
