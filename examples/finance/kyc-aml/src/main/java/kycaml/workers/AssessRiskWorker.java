package kycaml.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Assesses overall KYC risk based on identity verification and watchlist results.
 */
public class AssessRiskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "kyc_assess_risk";
    }

    @Override
    public TaskResult execute(Task task) {
        Object hitsObj = task.getInputData().get("watchlistHits");
        int hits = 0;
        if (hitsObj instanceof Number) {
            hits = ((Number) hitsObj).intValue();
        } else if (hitsObj instanceof String) {
            try { hits = Integer.parseInt((String) hitsObj); } catch (NumberFormatException ignored) {}
        }

        Object verifiedObj = task.getInputData().get("identityVerified");
        boolean verified = Boolean.TRUE.equals(verifiedObj)
                || "true".equals(String.valueOf(verifiedObj));

        int riskScore = 15;
        if (hits > 0) riskScore += 40;
        if (!verified) riskScore += 30;

        String riskLevel = riskScore >= 60 ? "high" : riskScore >= 30 ? "medium" : "low";

        System.out.println("  [risk] Score: " + riskScore + ", Level: " + riskLevel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("riskScore", riskScore);
        result.getOutputData().put("riskLevel", riskLevel);
        result.getOutputData().put("factors",
                List.of("nationality", "transaction_pattern", "document_verification"));
        return result;
    }
}
