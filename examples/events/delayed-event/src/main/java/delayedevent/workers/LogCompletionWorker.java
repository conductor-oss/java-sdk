package delayedevent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Logs the completion of the delayed event processing.
 * Input: eventId, processedAt
 * Output: logged (true)
 */
public class LogCompletionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "de_log_completion";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        String processedAt = (String) task.getInputData().get("processedAt");
        if (processedAt == null) {
            processedAt = "unknown";
        }

        System.out.println("  [de_log_completion] Event " + eventId + " completed at " + processedAt);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        return result;
    }
}
