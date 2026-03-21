package eventdrivenworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Handles payment-related events.
 * Input: eventData, priority
 * Output: handler ("payment"), processed (true), paymentId (from eventData)
 */
public class HandlePaymentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ed_handle_payment";
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

        String paymentId = eventData.get("paymentId") != null
                ? String.valueOf(eventData.get("paymentId"))
                : "unknown";

        System.out.println("  [ed_handle_payment] Processing payment " + paymentId + " (priority: " + priority + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handler", "payment");
        result.getOutputData().put("processed", true);
        result.getOutputData().put("paymentId", paymentId);
        return result;
    }
}
