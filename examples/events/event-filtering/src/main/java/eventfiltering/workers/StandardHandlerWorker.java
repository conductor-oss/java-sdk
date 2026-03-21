package eventfiltering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles standard (medium/low severity) events by queuing for batch processing.
 * Input:  eventId, payload, priority
 * Output: handled (true), handler ("standard"), queued (true), processedAt
 */
public class StandardHandlerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ef_standard_handler";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) eventId = "";

        System.out.println("  [ef_standard_handler] Standard processing for event " + eventId);
        System.out.println("  [ef_standard_handler] Queued for batch processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handled", true);
        result.getOutputData().put("handler", "standard");
        result.getOutputData().put("queued", true);
        result.getOutputData().put("processedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
