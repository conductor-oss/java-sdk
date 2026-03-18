package eventdrivensaga.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Ships an order after successful payment.
 * Input: orderId, address
 * Output: shipped (true), trackingNumber ("TRK-12345")
 */
public class ShipOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ds_ship_order";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        if (orderId == null) {
            orderId = "UNKNOWN";
        }

        String address = (String) task.getInputData().get("address");
        if (address == null) {
            address = "no address";
        }

        System.out.println("  [ds_ship_order] Shipping order " + orderId + " to " + address);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("shipped", true);
        result.getOutputData().put("trackingNumber", "TRK-12345");
        return result;
    }
}
