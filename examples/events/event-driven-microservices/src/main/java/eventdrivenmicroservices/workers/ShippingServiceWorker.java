package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Arranges shipping for an order.
 * Input: orderId, address, items
 * Output: trackingNumber ("SHIP-12345"), estimatedDelivery ("3-5 business days"), status ("label_created")
 */
public class ShippingServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_shipping_service";
    }

    @Override
    public TaskResult execute(Task task) {
        String address = (String) task.getInputData().get("address");
        if (address == null) {
            address = "unknown";
        }

        String trackingNumber = "SHIP-12345";

        System.out.println("  [shipping-svc] Created shipment " + trackingNumber
                + " to \"" + address + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("trackingNumber", trackingNumber);
        result.getOutputData().put("estimatedDelivery", "3-5 business days");
        result.getOutputData().put("status", "label_created");
        return result;
    }
}
