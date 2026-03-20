package eventdrivenworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Handles order-related events.
 * Input: eventData, priority
 * Output: handler ("order"), processed (true), orderId (from eventData)
 */
public class HandleOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ed_handle_order";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object eventDataRaw = task.getInputData().get("eventData");
        Map<String, Object> eventData = (eventDataRaw instanceof Map)
                ? (Map<String, Object>) eventDataRaw
                : Map.of();

        String priority = (String) task.getInputData().get("priority");
        if (priority == null) {
            priority = "normal";
        }

        String orderId = eventData.get("orderId") != null
                ? String.valueOf(eventData.get("orderId"))
                : "unknown";

        System.out.println("  [ed_handle_order] Processing order " + orderId + " (priority: " + priority + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "order");
        result.getOutputData().put("processed", true);
        result.getOutputData().put("orderId", orderId);
        return result;
    }
}
