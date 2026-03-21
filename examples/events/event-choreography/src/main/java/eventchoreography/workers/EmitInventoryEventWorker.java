package eventchoreography.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Emits an inventory event to the event bus.
 * Input: eventType, orderId
 * Output: emitted (true), eventType
 */
public class EmitInventoryEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ch_emit_inventory_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "unknown";
        }

        String orderId = (String) task.getInputData().get("orderId");

        System.out.println("  [ch_emit_inventory_event] Event: " + eventType + " for " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("emitted", true);
        result.getOutputData().put("eventType", eventType);
        return result;
    }
}
