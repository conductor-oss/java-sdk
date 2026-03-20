package purchaseorder.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CreateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "po_create";
    }

    @Override
    public TaskResult execute(Task task) {
        String vendor = (String) task.getInputData().get("vendor");
        Object amountObj = task.getInputData().get("totalAmount");
        double totalAmount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : 0;

        System.out.println("  [create] PO for " + vendor + ": $" + totalAmount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("poNumber", "PO-654-001");
        return result;
    }
}
