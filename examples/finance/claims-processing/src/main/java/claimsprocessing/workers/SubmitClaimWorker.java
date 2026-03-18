package claimsprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Submits an insurance claim with real validation.
 * Validates claim type, amount range, and generates a reference number.
 */
public class SubmitClaimWorker implements Worker {

    private static final Set<String> VALID_CLAIM_TYPES = Set.of(
            "auto_collision", "auto_comprehensive", "property_damage", "theft",
            "liability", "medical", "fire", "flood", "personal_injury");

    @Override
    public String getTaskDefName() { return "clp_submit_claim"; }

    @Override
    public TaskResult execute(Task task) {
        String claimId = (String) task.getInputData().get("claimId");
        String policyId = (String) task.getInputData().get("policyId");
        String claimType = (String) task.getInputData().get("claimType");
        Object amountObj = task.getInputData().get("amount");

        if (claimId == null) claimId = "UNKNOWN";
        if (policyId == null) policyId = "UNKNOWN";
        if (claimType == null) claimType = "unknown";

        double amount = 0;
        if (amountObj instanceof Number) amount = ((Number) amountObj).doubleValue();

        // Real validation
        boolean validType = VALID_CLAIM_TYPES.contains(claimType);
        boolean validAmount = amount > 0 && amount <= 1_000_000;

        // Determine policy status from policyId pattern
        String policyStatus = policyId.startsWith("POL-") ? "active" : "unknown";

        // Generate reference number from claim ID + date
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String referenceNumber = "CLM-" + today + "-" + Math.abs(claimId.hashCode() % 10000);

        System.out.println("  [submit] Claim " + claimId + " for policy " + policyId
                + " (type: " + claimType + ", amount: $" + (int) amount + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("policyStatus", policyStatus);
        result.getOutputData().put("submissionDate", LocalDate.now().toString());
        result.getOutputData().put("referenceNumber", referenceNumber);
        result.getOutputData().put("validType", validType);
        result.getOutputData().put("validAmount", validAmount);
        return result;
    }
}
