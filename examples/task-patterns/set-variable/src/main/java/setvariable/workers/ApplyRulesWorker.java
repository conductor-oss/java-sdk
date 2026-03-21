package setvariable.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Applies business rules based on intermediate state stored in workflow variables.
 *
 * Rules:
 * - needsApproval: totalAmount > 500 OR itemCount > 10
 * - riskLevel: high (total > 5000), medium (total > 1000), low (otherwise)
 */
public class ApplyRulesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sv_apply_rules";
    }

    @Override
    public TaskResult execute(Task task) {
        Object totalObj = task.getInputData().get("totalAmount");
        Object countObj = task.getInputData().get("itemCount");
        String category = (String) task.getInputData().get("category");

        double totalAmount = totalObj instanceof Number ? ((Number) totalObj).doubleValue() : 0;
        int itemCount = countObj instanceof Number ? ((Number) countObj).intValue() : 0;

        boolean needsApproval = totalAmount > 500 || itemCount > 10;

        String riskLevel;
        if (totalAmount > 5000) {
            riskLevel = "high";
        } else if (totalAmount > 1000) {
            riskLevel = "medium";
        } else {
            riskLevel = "low";
        }

        System.out.println("  [sv_apply_rules] category=" + category
                + ", needsApproval=" + needsApproval + ", riskLevel=" + riskLevel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("needsApproval", needsApproval);
        result.getOutputData().put("riskLevel", riskLevel);
        return result;
    }
}
