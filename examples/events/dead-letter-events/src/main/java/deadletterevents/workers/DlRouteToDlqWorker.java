package deadletterevents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Routes a failed event to the dead letter queue, producing a DLQ entry
 * with all relevant details.
 */
public class DlRouteToDlqWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dl_route_to_dlq";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        String eventType = (String) task.getInputData().get("eventType");
        Object payload = task.getInputData().get("payload");
        String errorReason = (String) task.getInputData().get("errorReason");
        Object retryCount = task.getInputData().get("retryCount");

        System.out.println("  [dl_route_to_dlq] Routing event " + eventId + " to DLQ");

        Map<String, Object> dlqEntry = Map.of(
                "eventId", eventId != null ? eventId : "",
                "eventType", eventType != null ? eventType : "",
                "payload", payload != null ? payload : Map.of(),
                "errorReason", errorReason != null ? errorReason : "",
                "retryCount", retryCount != null ? retryCount : 0,
                "routedAt", "2026-01-15T10:00:00Z"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dlqId", "dlq-fixed-001");
        result.getOutputData().put("dlqEntry", dlqEntry);
        result.getOutputData().put("routedToDlq", true);
        return result;
    }
}
