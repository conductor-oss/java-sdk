package eventpriority.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Classifies an event into a priority level based on its type.
 * Input: eventId, eventType
 * Output: priority, eventType
 *
 * Priority mapping:
 *   "payment.failed" -> "high"
 *   "order.created"  -> "medium"
 *   anything else    -> "low"
 */
public class ClassifyPriorityWorker implements Worker {

    private static final Map<String, String> PRIORITY_MAP = Map.of(
            "payment.failed", "high",
            "order.created", "medium"
    );

    @Override
    public String getTaskDefName() {
        return "pr_classify_priority";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "unknown";
        }

        System.out.println("  [pr_classify_priority] Classifying event type: " + eventType);

        String priority = PRIORITY_MAP.getOrDefault(eventType, "low");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("priority", priority);
        result.getOutputData().put("eventType", eventType);
        return result;
    }
}
