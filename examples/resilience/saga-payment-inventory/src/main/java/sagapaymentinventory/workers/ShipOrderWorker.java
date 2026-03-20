package sagapaymentinventory.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for spi_ship_order -- ships an order.
 *
 * If the input parameter "shouldFail" is true, the worker returns shipStatus="failed".
 * Otherwise, it returns shipStatus="shipped" with a deterministic shipment ID "SHIP-001".
 *
 * The task always completes (COMPLETED status) -- the SWITCH task in the workflow
 * inspects the shipStatus output to decide whether to run compensation.
 */
public class ShipOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "spi_ship_order";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().getOrDefault("orderId", "unknown");
        Object shouldFailInput = task.getInputData().get("shouldFail");
        boolean shouldFail = Boolean.TRUE.equals(shouldFailInput)
                || "true".equals(String.valueOf(shouldFailInput));

        System.out.println("  [spi_ship_order] Shipping order: " + orderId + " (shouldFail=" + shouldFail + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderId", orderId);

        if (shouldFail) {
            result.getOutputData().put("shipmentId", "SHIP-001");
            result.getOutputData().put("shipStatus", "failed");
        } else {
            result.getOutputData().put("shipmentId", "SHIP-001");
            result.getOutputData().put("shipStatus", "shipped");
        }

        return result;
    }
}
