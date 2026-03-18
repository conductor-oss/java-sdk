package procurementworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Creates a purchase order. Real PO generation with unique numbers.
 */
public class PurchaseWorker implements Worker {
    @Override public String getTaskDefName() { return "prw_purchase"; }

    @Override public TaskResult execute(Task task) {
        String requisitionId = (String) task.getInputData().get("requisitionId");
        Object approvedObj = task.getInputData().get("approved");
        Object costObj = task.getInputData().get("approvedAmount");
        if (requisitionId == null) requisitionId = "UNKNOWN";
        boolean approved = Boolean.TRUE.equals(approvedObj);

        double totalCost = costObj instanceof Number ? ((Number) costObj).doubleValue() : 0;
        String poNumber = "PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [purchase] " + (approved ? "PO " + poNumber + " created" : "Requisition rejected")
                + " for " + requisitionId + " ($" + (int) totalCost + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("poNumber", approved ? poNumber : "NONE");
        result.getOutputData().put("totalCost", totalCost);
        result.getOutputData().put("createdAt", Instant.now().toString());
        return result;
    }
}
