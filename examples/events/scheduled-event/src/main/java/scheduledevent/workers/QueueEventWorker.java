package scheduledevent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Queues an event for scheduled execution.
 * Input: eventId, payload, scheduledTime
 * Output: queued (true), eventId, scheduledTime
 */
public class QueueEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "se_queue_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        Object payload = task.getInputData().get("payload");
        if (payload == null) {
            payload = java.util.Map.of();
        }

        String scheduledTime = (String) task.getInputData().get("scheduledTime");
        if (scheduledTime == null) {
            scheduledTime = "1970-01-01T00:00:00Z";
        }

        System.out.println("  [se_queue_event] Queuing event: " + eventId + " for " + scheduledTime);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("queued", true);
        result.getOutputData().put("eventId", eventId);
        result.getOutputData().put("scheduledTime", scheduledTime);
        return result;
    }
}
