package eventfiltering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles urgent (critical/high severity) events with immediate alerting.
 * Input:  eventId, payload, priority
 * Output: handled (true), handler ("urgent"), alertSent (true), escalated (true), processedAt
 */
public class UrgentHandlerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ef_urgent_handler";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) eventId = "";

        System.out.println("  [ef_urgent_handler] URGENT processing for event " + eventId + "!");
        System.out.println("  [ef_urgent_handler] Triggering immediate alert and escalation...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handled", true);
        result.getOutputData().put("handler", "urgent");
        result.getOutputData().put("alertSent", true);
        result.getOutputData().put("escalated", true);
        result.getOutputData().put("processedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
