package eventttl.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Acknowledges a successfully processed event.
 * Input: eventId
 * Output: acknowledged (true)
 */
public class AcknowledgeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xl_acknowledge";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        System.out.println("  [xl_acknowledge] Acknowledged event " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("acknowledged", true);
        return result;
    }
}
