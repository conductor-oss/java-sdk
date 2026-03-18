package eventrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Default processor for events that do not match user or order domains.
 * Passes through the domain and marks the event as processed.
 */
public class SystemProcessorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eo_system_processor";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        String domain = (String) task.getInputData().get("domain");

        if (eventId == null) eventId = "unknown";
        if (domain == null) domain = "unknown";

        System.out.println("  [eo_system_processor] Processing system event: " + eventId
                + " domain=" + domain);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("processor", "system");
        result.getOutputData().put("domain", domain);
        result.getOutputData().put("processedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
