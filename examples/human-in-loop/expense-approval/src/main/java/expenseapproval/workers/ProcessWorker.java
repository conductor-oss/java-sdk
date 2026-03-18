package expenseapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Processes the approved expense. Real processing:
 * generates transaction ID, computes reimbursement timeline.
 */
public class ProcessWorker implements Worker {
    @Override public String getTaskDefName() { return "exp_process"; }

    @Override public TaskResult execute(Task task) {
        Object amountObj = task.getInputData().get("amount");
        String category = (String) task.getInputData().get("category");
        if (category == null) category = "general";

        double amount = 0;
        if (amountObj instanceof Number) amount = ((Number) amountObj).doubleValue();

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Real reimbursement timeline based on amount
        int daysToReimburse = amount > 5000 ? 14 : amount > 500 ? 7 : 3;

        System.out.println("  [exp_process] Processing $" + String.format("%.2f", amount)
                + " (" + category + ") -> " + transactionId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("transactionId", transactionId);
        result.getOutputData().put("reimbursementDays", daysToReimburse);
        result.getOutputData().put("processedAt", Instant.now().toString());
        return result;
    }
}
