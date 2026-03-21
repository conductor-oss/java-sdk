package eventttl.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks whether an event has exceeded its TTL.
 * Input: eventId, ttlSeconds, createdAt
 * Output: status ("valid" or "expired"), ageSeconds, ttlSeconds
 *
 * Deterministic logic: always returns status "valid" with ageSeconds=120.
 */
public class CheckExpiryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xl_check_expiry";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        Object ttlRaw = task.getInputData().get("ttlSeconds");
        int ttlSeconds = 300;
        if (ttlRaw instanceof Number) {
            ttlSeconds = ((Number) ttlRaw).intValue();
        } else if (ttlRaw instanceof String) {
            try {
                ttlSeconds = Integer.parseInt((String) ttlRaw);
            } catch (NumberFormatException ignored) {
            }
        }

        int ageSeconds = 120;
        String status = "valid";

        System.out.println("  [xl_check_expiry] Event " + eventId
                + ": age=" + ageSeconds + "s, TTL=" + ttlSeconds + "s -> " + status);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", status);
        result.getOutputData().put("ageSeconds", ageSeconds);
        result.getOutputData().put("ttlSeconds", ttlSeconds);
        return result;
    }
}
