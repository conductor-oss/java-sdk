package claimsprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies policy details for a claim.
 * Real logic: checks policy status, computes coverage limit from policy ID hash.
 */
public class VerifyDetailsWorker implements Worker {

    @Override
    public String getTaskDefName() { return "clp_verify_details"; }

    @Override
    public TaskResult execute(Task task) {
        String policyId = (String) task.getInputData().get("policyId");
        String policyStatus = (String) task.getInputData().get("policyStatus");
        if (policyId == null) policyId = "UNKNOWN";
        if (policyStatus == null) policyStatus = "unknown";

        // Real verification: policy must be active
        boolean verified = "active".equalsIgnoreCase(policyStatus);

        // Derive coverage details from policy ID (deterministic)
        int policyHash = Math.abs(policyId.hashCode());
        int coverageLimit = ((policyHash % 10) + 1) * 10000; // 10000-100000 range
        int deductible = ((policyHash / 10) % 5 + 1) * 100;  // 100-500 range

        // Derive policy holder name from policy ID
        String policyHolder = "Policyholder-" + policyId;

        System.out.println("  [verify] Policy " + policyId + " - status: " + policyStatus
                + ", verified: " + verified + ", coverage: $" + coverageLimit + ", deductible: $" + deductible);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", verified);
        result.getOutputData().put("policyHolder", policyHolder);
        result.getOutputData().put("coverageLimit", coverageLimit);
        result.getOutputData().put("deductible", deductible);
        return result;
    }
}
