package webhookratelimiting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Queues a throttled webhook request for later retry.
 * Input: senderId, retryAfterMs
 * Output: queued (true), retryAfterMs
 */
public class QueueThrottledWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wl_queue_throttled";
    }

    @Override
    public TaskResult execute(Task task) {
        String senderId = (String) task.getInputData().get("senderId");
        if (senderId == null) {
            senderId = "unknown";
        }

        Object retryAfterObj = task.getInputData().get("retryAfterMs");
        int retryAfterMs = 60000;
        if (retryAfterObj instanceof Number) {
            retryAfterMs = ((Number) retryAfterObj).intValue();
        } else if (retryAfterObj instanceof String) {
            try {
                retryAfterMs = Integer.parseInt((String) retryAfterObj);
            } catch (NumberFormatException ignored) {
            }
        }

        System.out.println("  [wl_queue_throttled] Throttled " + senderId
                + ", retry after " + retryAfterMs + "ms");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("queued", true);
        result.getOutputData().put("retryAfterMs", retryAfterMs);
        return result;
    }
}
