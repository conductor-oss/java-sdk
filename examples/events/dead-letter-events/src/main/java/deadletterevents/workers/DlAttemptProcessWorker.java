package deadletterevents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Attempts to process an event. If the payload contains a "requiredField" key,
 * processing succeeds. Otherwise it fails with a descriptive error reason.
 */
public class DlAttemptProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dl_attempt_process";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        String eventType = (String) task.getInputData().get("eventType");
        Object payloadRaw = task.getInputData().get("payload");
        Object retryCountRaw = task.getInputData().get("retryCount");

        System.out.println("  [dl_attempt_process] Attempting to process event: " + eventId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        boolean hasRequiredField = false;
        if (payloadRaw instanceof Map) {
            Map<String, Object> payload = (Map<String, Object>) payloadRaw;
            hasRequiredField = payload.containsKey("requiredField");
        }

        if (!hasRequiredField) {
            result.getOutputData().put("processingResult", "failed");
            result.getOutputData().put("errorReason",
                    "Missing required field 'requiredField' in payload");
            result.getOutputData().put("failedAt", "2026-01-15T10:00:00Z");
        } else {
            result.getOutputData().put("processingResult", "success");
            result.getOutputData().put("resultData", Map.of("processed", true));
            result.getOutputData().put("errorReason", null);
        }
        return result;
    }
}
