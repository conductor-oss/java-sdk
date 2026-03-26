package teamsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Formats an adaptive card for Teams from event data.
 * Input: eventType, data
 * Output: card
 */
public class FormatCardWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tms_format_card";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null) {
            eventType = "unknown";
        }
        Object data = task.getInputData().get("data");
        String dataStr = data != null ? data.toString() : "{}";

        Map<String, Object> card = Map.of(
                "type", "AdaptiveCard",
                "version", "1.4",
                "body", List.of(Map.of("type", "TextBlock", "text", "Alert: " + dataStr)));

        System.out.println("  [format] Created adaptive card for " + eventType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("card", card);
        return result;
    }
}
