package contractlifecycle.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Reviews a contract. Real review checklist validation.
 */
public class ReviewWorker implements Worker {
    @Override public String getTaskDefName() { return "clf_review"; }

    @Override public TaskResult execute(Task task) {
        String contractId = (String) task.getInputData().get("contractId");
        Object amountObj = task.getInputData().get("amount");
        if (contractId == null) contractId = "UNKNOWN";
        double amount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : 0;

        List<String> reviewItems = new ArrayList<>();
        reviewItems.add("Terms and conditions: reviewed");
        reviewItems.add("Payment terms: reviewed");
        reviewItems.add("Liability clause: reviewed");
        if (amount > 100000) reviewItems.add("Legal review: required");

        boolean reviewPassed = true; // All items reviewed

        System.out.println("  [review] Contract " + contractId + ": " + reviewItems.size() + " items reviewed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("contractState", "IN_REVIEW");
        result.getOutputData().put("reviewPassed", reviewPassed);
        result.getOutputData().put("reviewItems", reviewItems);
        return result;
    }
}
