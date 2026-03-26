package eventdrivenworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Classifies an event by its type into a category and priority.
 * Input: eventType, metadata
 * Output: category, priority
 *
 * Classification logic:
 *   "order.created" or "order.updated" -> category "order"
 *   "payment.received" or "payment.refunded" -> category "payment"
 *   anything else -> category "generic"
 *
 * Priority: payment -> "high", else -> "normal"
 */
public class ClassifyEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ed_classify_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "unknown";
        }

        System.out.println("  [ed_classify_event] Classifying event type: " + eventType);

        String category;
        String priority;

        if ("order.created".equals(eventType) || "order.updated".equals(eventType)) {
            category = "order";
            priority = "normal";
        } else if ("payment.received".equals(eventType) || "payment.refunded".equals(eventType)) {
            category = "payment";
            priority = "high";
        } else {
            category = "generic";
            priority = "normal";
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("category", category);
        result.getOutputData().put("priority", priority);
        return result;
    }
}
