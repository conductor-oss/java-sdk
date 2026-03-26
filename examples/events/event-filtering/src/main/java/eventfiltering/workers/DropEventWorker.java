package eventfiltering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles events that are dropped due to unknown severity levels.
 * Input:  eventId, reason
 * Output: handled (false), handler ("drop"), reason
 */
public class DropEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ef_drop_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        String reason = (String) task.getInputData().get("reason");

        if (eventId == null) eventId = "";
        if (reason == null) reason = "";

        System.out.println("  [ef_drop_event] Dropping event " + eventId + ": " + reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handled", false);
        result.getOutputData().put("handler", "drop");
        result.getOutputData().put("reason", reason);
        return result;
    }
}
