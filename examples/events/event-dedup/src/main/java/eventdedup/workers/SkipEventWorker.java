package eventdedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Skips a duplicate (or unknown-status) event.
 *
 * Input:  {eventId, hash, reason}
 * Output: {skipped: true, eventId, reason}
 */
public class SkipEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dd_skip_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        String hash = (String) task.getInputData().get("hash");
        String reason = (String) task.getInputData().get("reason");

        System.out.println("  [dd_skip_event] Skipping event: " + eventId + " — " + reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("skipped", true);
        result.getOutputData().put("eventId", eventId);
        result.getOutputData().put("reason", reason);
        return result;
    }
}
