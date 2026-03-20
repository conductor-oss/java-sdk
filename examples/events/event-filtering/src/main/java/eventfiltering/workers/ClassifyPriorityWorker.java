package eventfiltering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Classifies an event into a priority level based on severity.
 * Input:  eventType, severity, metadata
 * Output: priority ("urgent" | "standard" | "drop"),
 *         filterResult ("routed_to_urgent" | "routed_to_standard" | "dropped"),
 *         dropReason (only when priority is "drop")
 */
public class ClassifyPriorityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ef_classify_priority";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        String severity = (String) task.getInputData().get("severity");

        if (eventType == null) eventType = "";
        if (severity == null) severity = "";

        System.out.println("  [ef_classify_priority] Classifying event: type="
                + eventType + ", severity=" + severity + "...");

        String priority;
        String filterResult;
        String dropReason = null;

        if ("critical".equals(severity) || "high".equals(severity)) {
            priority = "urgent";
            filterResult = "routed_to_urgent";
        } else if ("medium".equals(severity) || "low".equals(severity)) {
            priority = "standard";
            filterResult = "routed_to_standard";
        } else {
            priority = "drop";
            filterResult = "dropped";
            dropReason = "Unknown severity level: " + severity;
        }

        System.out.println("  [ef_classify_priority] Priority: " + priority
                + ", result: " + filterResult);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("priority", priority);
        result.getOutputData().put("filterResult", filterResult);
        result.getOutputData().put("dropReason", dropReason);
        return result;
    }
}
