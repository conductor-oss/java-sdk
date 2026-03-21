package eventdedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a new (non-duplicate) event.
 *
 * Input:  {eventId, payload, hash}
 * Output: {processed: true, eventId}
 */
public class ProcessEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dd_process_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        Object payload = task.getInputData().get("payload");
        String hash = (String) task.getInputData().get("hash");

        System.out.println("  [dd_process_event] Processing event: " + eventId + " (hash=" + hash + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("eventId", eventId);
        return result;
    }
}
