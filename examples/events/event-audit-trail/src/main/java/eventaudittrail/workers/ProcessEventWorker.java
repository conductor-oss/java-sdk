package eventaudittrail.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes an event.
 * Input: eventId, eventData
 * Output: result ("success"), eventId
 */
public class ProcessEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "at_process_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        Object eventData = task.getInputData().get("eventData");

        System.out.println("  [at_process_event] Processing event: " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "success");
        result.getOutputData().put("eventId", eventId);
        return result;
    }
}
