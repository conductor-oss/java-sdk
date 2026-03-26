package sagapaymentinventory.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for spi_reserve_inventory -- reserves inventory for an order.
 *
 * Produces a deterministic reservation ID "INV-001" and marks the task as COMPLETED.
 */
public class ReserveInventoryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "spi_reserve_inventory";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().getOrDefault("orderId", "unknown");

        System.out.println("  [spi_reserve_inventory] Reserving inventory for order: " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reservationId", "INV-001");
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("inventoryStatus", "reserved");

        return result;
    }
}
