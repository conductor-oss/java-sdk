package sagapaymentinventory.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for spi_release_inventory -- compensation worker that releases reserved inventory.
 *
 * This is called as part of the saga compensation when shipping fails.
 */
public class ReleaseInventoryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "spi_release_inventory";
    }

    @Override
    public TaskResult execute(Task task) {
        String reservationId = (String) task.getInputData().getOrDefault("reservationId", "unknown");
        String orderId = (String) task.getInputData().getOrDefault("orderId", "unknown");

        System.out.println("  [spi_release_inventory] Releasing inventory " + reservationId + " for order: " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reservationId", reservationId);
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("inventoryStatus", "released");

        return result;
    }
}
