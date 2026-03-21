package slackintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a Slack event and produces a formatted message.
 * Input: eventType, data
 * Output: message, processed
 *
 * This worker is always deterministic.— event processing is an internal
 * business-logic step that does not require an external API call.
 */
public class ProcessEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "slk_process_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "unknown";
        }
        Object data = task.getInputData().get("data");
        String dataStr = data != null ? data.toString() : "{}";

        String message = "Processed " + eventType + ": " + dataStr;

        System.out.println("  [process] " + message);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("message", message);
        result.getOutputData().put("processed", true);
        return result;
    }
}
