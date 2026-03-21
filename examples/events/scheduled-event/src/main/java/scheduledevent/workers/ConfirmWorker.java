package scheduledevent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Confirms that the scheduled event was executed successfully.
 * Input: eventId, executedAt
 * Output: confirmed (true)
 */
public class ConfirmWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "se_confirm";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        String executedAt = (String) task.getInputData().get("executedAt");
        if (executedAt == null) {
            executedAt = "unknown";
        }

        System.out.println("  [se_confirm] Confirming event: " + eventId + " executed at " + executedAt);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("confirmed", true);
        return result;
    }
}
