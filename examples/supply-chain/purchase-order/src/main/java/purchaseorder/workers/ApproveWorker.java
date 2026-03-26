package purchaseorder.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApproveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "po_approve";
    }

    @Override
    public TaskResult execute(Task task) {
        String poNumber = (String) task.getInputData().get("poNumber");
        Object amountObj = task.getInputData().get("totalAmount");
        double totalAmount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : 0;
        boolean approved = totalAmount <= 100000;

        System.out.println("  [approve] " + poNumber + ": " + (approved ? "approved" : "needs escalation"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", approved);
        result.getOutputData().put("approver", "procurement-mgr");
        return result;
    }
}
