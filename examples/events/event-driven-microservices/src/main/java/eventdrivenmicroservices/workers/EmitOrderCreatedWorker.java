package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Emits the order.created event.
 * Input: eventType, orderId, amount
 * Output: published (true), eventType
 */
public class EmitOrderCreatedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_emit_order_created";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "order.created";
        }

        String orderId = task.getInputData().get("orderId") != null
                ? String.valueOf(task.getInputData().get("orderId"))
                : "unknown";

        System.out.println("  [event-bus] Published: " + eventType
                + " (order: " + orderId + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("published", true);
        result.getOutputData().put("eventType", eventType);
        return result;
    }
}
