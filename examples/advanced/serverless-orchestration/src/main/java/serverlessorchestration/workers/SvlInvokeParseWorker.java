package serverlessorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Invokes the parse serverless function for an incoming event.
 * Input: functionArn, eventId, payload
 * Output: parsed (type, page, userId), billedMs
 */
public class SvlInvokeParseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "svl_invoke_parse";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        System.out.println("  [lambda:parse] Invoking for event " + eventId);

        Map<String, Object> parsed = Map.of(
                "type", "click",
                "page", "/products",
                "userId", "U-123"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("parsed", parsed);
        result.getOutputData().put("billedMs", 45);
        return result;
    }
}
