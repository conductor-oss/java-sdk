package setvariable.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Produces the final decision string from all accumulated workflow variables.
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sv_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        Object totalObj = task.getInputData().get("totalAmount");
        Object countObj = task.getInputData().get("itemCount");
        String category = (String) task.getInputData().get("category");
        Object approvalObj = task.getInputData().get("needsApproval");
        String riskLevel = (String) task.getInputData().get("riskLevel");

        double totalAmount = totalObj instanceof Number ? ((Number) totalObj).doubleValue() : 0;
        int itemCount = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
        boolean needsApproval = Boolean.TRUE.equals(approvalObj);

        String decision = String.format(
                "Order of %d items ($%.2f) classified as %s, risk=%s, approval=%s",
                itemCount, totalAmount, category, riskLevel,
                needsApproval ? "required" : "not-required"
        );

        System.out.println("  [sv_finalize] " + decision);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decision", decision);
        result.getOutputData().put("totalAmount", totalAmount);
        result.getOutputData().put("itemCount", itemCount);
        result.getOutputData().put("category", category);
        result.getOutputData().put("needsApproval", needsApproval);
        result.getOutputData().put("riskLevel", riskLevel);
        return result;
    }
}
