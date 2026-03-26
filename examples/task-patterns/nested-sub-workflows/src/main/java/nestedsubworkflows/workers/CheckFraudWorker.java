package nestedsubworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks fraud risk for a transaction.
 * Returns a deterministic risk score (25) and approval status.
 * Used by the deepest sub-workflow (Level 3: nested_fraud_check).
 */
public class CheckFraudWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nest_check_fraud";
    }

    @Override
    public TaskResult execute(Task task) {
        String email = (String) task.getInputData().get("email");
        Object amountObj = task.getInputData().get("amount");

        if (email == null || email.isBlank()) {
            email = "unknown@example.com";
        }

        double amount = 0.0;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        }

        System.out.println("  [nest_check_fraud] Checking fraud for email=" + email + " amount=" + amount);

        // Deterministic risk score (no randomness)
        int riskScore = 25;
        boolean approved = riskScore < 80;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("riskScore", riskScore);
        result.getOutputData().put("approved", approved);
        return result;
    }
}
