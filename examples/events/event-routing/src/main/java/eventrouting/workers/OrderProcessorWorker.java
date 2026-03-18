package eventrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes order-domain events. Extracts the orderId from eventData and
 * returns a fixed result indicating fulfillment has started.
 */
public class OrderProcessorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eo_order_processor";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        String subType = (String) task.getInputData().get("subType");
        Object eventDataObj = task.getInputData().get("eventData");

        if (eventId == null) eventId = "unknown";
        if (subType == null) subType = "unknown";

        String orderId = "unknown";
        if (eventDataObj instanceof Map) {
            Map<String, Object> eventData = (Map<String, Object>) eventDataObj;
            Object orderIdObj = eventData.get("orderId");
            if (orderIdObj != null) {
                orderId = orderIdObj.toString();
            }
        }

        System.out.println("  [eo_order_processor] Processing order event: " + eventId
                + " orderId=" + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("processor", "order");
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("fulfillmentStarted", true);
        result.getOutputData().put("processedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
