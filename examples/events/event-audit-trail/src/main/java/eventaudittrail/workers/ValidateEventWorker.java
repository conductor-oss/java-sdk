package eventaudittrail.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validates an incoming event.
 * Input: eventId, eventData
 * Output: valid (true), eventId
 */
public class ValidateEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "at_validate_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        Object eventData = task.getInputData().get("eventData");

        System.out.println("  [at_validate_event] Validating event: " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("eventId", eventId);
        return result;
    }
}
