package legalcontractreview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for lcr_finalize task -- finalizes the legal contract review.
 *
 * Called after the human legal review (WAIT task) is completed.
 * Records the contract ID and the number of risk flags that were reviewed.
 *
 * Outputs:
 *   - finalized: true
 *   - contractId: the contract identifier
 *   - riskFlagCount: number of risk flags from the extraction step
 */
public class LcrFinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lcr_finalize";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object rawContractId = task.getInputData().get("contractId");
        String contractId = (rawContractId instanceof String) ? (String) rawContractId : "unknown";

        Object rawFlags = task.getInputData().get("riskFlags");
        int riskFlagCount = 0;
        if (rawFlags instanceof java.util.List) {
            riskFlagCount = ((java.util.List<?>) rawFlags).size();
        }

        System.out.println("  [lcr_finalize] Finalizing contract " + contractId
                + " (" + riskFlagCount + " risk flags reviewed)...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalized", true);
        result.getOutputData().put("contractId", contractId);
        result.getOutputData().put("riskFlagCount", riskFlagCount);

        System.out.println("  [lcr_finalize] Contract review finalized.");
        return result;
    }
}
