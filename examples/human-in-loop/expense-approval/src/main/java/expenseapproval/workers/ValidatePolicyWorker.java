package expenseapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for exp_validate_policy task -- validates expense against policy rules.
 *
 * Policy rules:
 * - If amount > 100 OR category equals "travel", approvalRequired = "true"
 * - Otherwise, approvalRequired = "false"
 *
 * Inputs: amount (Number), category (String)
 * Outputs: approvalRequired ("true" or "false")
 */
public class ValidatePolicyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "exp_validate_policy";
    }

    @Override
    public TaskResult execute(Task task) {
        double amount = 0;
        Object amountInput = task.getInputData().get("amount");
        if (amountInput instanceof Number) {
            amount = ((Number) amountInput).doubleValue();
        }

        String category = "";
        Object categoryInput = task.getInputData().get("category");
        if (categoryInput instanceof String) {
            category = (String) categoryInput;
        }

        boolean needsApproval = amount > 100 || "travel".equals(category);
        String approvalRequired = needsApproval ? "true" : "false";

        System.out.println("  [exp_validate_policy] amount=" + amount
                + ", category=" + category
                + " -> approvalRequired=" + approvalRequired);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approvalRequired", approvalRequired);
        return result;
    }
}
