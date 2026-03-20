package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Emits the shipment.created event.
 * Input: eventType, orderId, trackingNumber
 * Output: published (true), eventType
 */
public class EmitShipmentCreatedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_emit_shipment_created";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "shipment.created";
        }

        String trackingNumber = task.getInputData().get("trackingNumber") != null
                ? String.valueOf(task.getInputData().get("trackingNumber"))
                : "unknown";

        System.out.println("  [event-bus] Published: " + eventType
                + " (tracking: " + trackingNumber + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("published", true);
        result.getOutputData().put("eventType", eventType);
        return result;
    }
}
