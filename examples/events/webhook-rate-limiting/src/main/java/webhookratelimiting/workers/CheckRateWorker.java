package webhookratelimiting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks the current request rate for a sender against the rate limit.
 * Input: senderId, rateLimit
 * Output: decision ("allowed" or "throttled"), currentRate, limit, retryAfterMs
 *
 * Logic:
 *   Performs a current rate of 45 requests/min.
 *   If currentRate < limit -> "allowed", retryAfterMs = 0
 *   If currentRate >= limit -> "throttled", retryAfterMs = 60000
 */
public class CheckRateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wl_check_rate";
    }

    @Override
    public TaskResult execute(Task task) {
        String senderId = (String) task.getInputData().get("senderId");
        if (senderId == null) {
            senderId = "unknown";
        }

        int limit = 100;
        Object rateLimitObj = task.getInputData().get("rateLimit");
        if (rateLimitObj instanceof Number) {
            limit = ((Number) rateLimitObj).intValue();
        } else if (rateLimitObj instanceof String) {
            try {
                limit = Integer.parseInt((String) rateLimitObj);
            } catch (NumberFormatException ignored) {
            }
        }

        int currentRate = 45; // deterministic current rate
        String decision = currentRate < limit ? "allowed" : "throttled";
        int retryAfterMs = "throttled".equals(decision) ? 60000 : 0;

        System.out.println("  [wl_check_rate] Sender " + senderId + ": "
                + currentRate + "/" + limit + " req/min -> " + decision);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decision", decision);
        result.getOutputData().put("currentRate", currentRate);
        result.getOutputData().put("limit", limit);
        result.getOutputData().put("retryAfterMs", retryAfterMs);
        return result;
    }
}
