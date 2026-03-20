package toolratelimit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Queues a throttled request for later execution.
 * Returns a fixed queueId and the estimated wait time.
 */
public class QueueRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rl_queue_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        Object retryAfterMsRaw = task.getInputData().get("retryAfterMs");
        int retryAfterMs = 5000;
        if (retryAfterMsRaw instanceof Number) {
            retryAfterMs = ((Number) retryAfterMsRaw).intValue();
        }

        Object queuePositionRaw = task.getInputData().get("queuePosition");
        int queuePosition = 1;
        if (queuePositionRaw instanceof Number) {
            queuePosition = ((Number) queuePositionRaw).intValue();
        }

        System.out.println("  [rl_queue_request] Queuing request for tool: " + toolName
                + " (position " + queuePosition + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("queueId", "q-fixed-001");
        result.getOutputData().put("queuePosition", queuePosition);
        result.getOutputData().put("estimatedWaitMs", retryAfterMs);
        result.getOutputData().put("queuedAt", "2026-03-08T10:00:00Z");
        result.getOutputData().put("status", "queued");
        return result;
    }
}
