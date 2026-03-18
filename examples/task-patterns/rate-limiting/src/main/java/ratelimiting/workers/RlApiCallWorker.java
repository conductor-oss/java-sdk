package ratelimiting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Rate-limited API call worker.
 * Takes a batchId and returns a completion result.
 *
 * The rate limiting is configured at the task definition level:
 * - rateLimitPerFrequency: 5 (max 5 executions per window)
 * - rateLimitFrequencyInSeconds: 10 (10-second window)
 * - concurrentExecLimit: 2 (max 2 running at once)
 */
public class RlApiCallWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rl_api_call";
    }

    @Override
    public TaskResult execute(Task task) {
        Object batchIdRaw = task.getInputData().get("batchId");
        String batchId;
        if (batchIdRaw == null) {
            batchId = "unknown";
        } else {
            batchId = String.valueOf(batchIdRaw);
            if (batchId.isBlank()) {
                batchId = "unknown";
            }
        }

        System.out.println("  [rl_api_call] Processing batch: " + batchId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "batch-" + batchId + "-done");
        return result;
    }
}
