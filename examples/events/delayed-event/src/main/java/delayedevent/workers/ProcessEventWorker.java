package delayedevent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes the delayed event.
 * Input: eventId, payload
 * Output: processedAt (timestamp), result ("success")
 */
public class ProcessEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "de_process_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        String processedAt = "2026-01-15T10:00:05Z";

        System.out.println("  [de_process_event] Processing delayed event " + eventId + " at " + processedAt);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedAt", processedAt);
        result.getOutputData().put("result", "success");
        return result;
    }
}
