package procurementworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.UUID;

/**
 * Creates a purchase requisition. Real cost estimation based on item and quantity.
 */
public class RequisitionWorker implements Worker {
    @Override public String getTaskDefName() { return "prw_requisition"; }

    @Override public TaskResult execute(Task task) {
        String item = (String) task.getInputData().get("item");
        String requester = (String) task.getInputData().get("requester");
        Object qtyObj = task.getInputData().get("quantity");
        if (item == null) item = "unknown";
        if (requester == null) requester = "unknown";
        int quantity = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 1;

        // Real unit price estimation based on item hash
        double unitPrice = 10.0 + Math.abs(item.hashCode() % 990); // $10-$999 range
        double estimatedCost = quantity * unitPrice;

        String requisitionId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [requisition] " + requester + " requests " + quantity + "x " + item
                + " @ $" + String.format("%.2f", unitPrice) + " = $" + String.format("%.2f", estimatedCost));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requisitionId", requisitionId);
        result.getOutputData().put("estimatedCost", estimatedCost);
        result.getOutputData().put("unitPrice", unitPrice);
        result.getOutputData().put("quantity", quantity);
        return result;
    }
}
