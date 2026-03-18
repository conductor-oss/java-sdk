package eventrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes user-domain events. Returns a fixed result indicating the user
 * event was handled: profile updated, notification sent, and audit logged.
 */
public class UserProcessorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eo_user_processor";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        String subType = (String) task.getInputData().get("subType");

        if (eventId == null) eventId = "unknown";
        if (subType == null) subType = "unknown";

        System.out.println("  [eo_user_processor] Processing user event: " + eventId
                + " subType=" + subType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("processor", "user");
        result.getOutputData().put("actions", Map.of(
                "profileUpdated", true,
                "notificationSent", true,
                "auditLogged", true
        ));
        result.getOutputData().put("processedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
