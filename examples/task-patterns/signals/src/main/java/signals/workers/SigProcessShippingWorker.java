package signals.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes shipping information after the shipping signal is received.
 * Takes orderId, trackingNumber, and carrier. Returns { notified: true }.
 */
public class SigProcessShippingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sig_process_shipping";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        String trackingNumber = (String) task.getInputData().get("trackingNumber");
        String carrier = (String) task.getInputData().get("carrier");

        if (orderId == null || orderId.isBlank()) {
            orderId = "unknown";
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            trackingNumber = "N/A";
        }
        if (carrier == null || carrier.isBlank()) {
            carrier = "N/A";
        }

        System.out.println("  [sig_process_shipping] Processing shipping for order: " + orderId
                + " | tracking: " + trackingNumber + " | carrier: " + carrier);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("trackingNumber", trackingNumber);
        result.getOutputData().put("carrier", carrier);
        result.getOutputData().put("notified", true);
        return result;
    }
}
