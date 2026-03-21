package eventttl.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a valid (non-expired) event.
 * Input: eventId, payload
 * Output: processed (true)
 */
public class ProcessEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xl_process_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        System.out.println("  [xl_process_event] Processing valid event " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        return result;
    }
}
