package exponentialmaxretry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for emr_unreliable_api — runs an unreliable API call.
 *
 * If shouldSucceed=true, returns success with status="API call successful".
 * Otherwise, returns FAILED with error details.
 *
 * The task definition is configured with:
 * - retryCount: 3
 * - retryLogic: EXPONENTIAL_BACKOFF
 * - retryDelaySeconds: 1
 */
public class UnreliableApiWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "emr_unreliable_api";
    }

    @Override
    public TaskResult execute(Task task) {
        Object shouldSucceedInput = task.getInputData().get("shouldSucceed");
        boolean shouldSucceed = Boolean.TRUE.equals(shouldSucceedInput)
                || "true".equals(String.valueOf(shouldSucceedInput));

        System.out.println("  [emr_unreliable_api] shouldSucceed=" + shouldSucceed);

        TaskResult result = new TaskResult(task);

        if (shouldSucceed) {
            System.out.println("  [emr_unreliable_api] API call successful");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("status", "API call successful");
            result.getOutputData().put("data", "processed");
        } else {
            System.out.println("  [emr_unreliable_api] API call failed");
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "API call failed");
            result.getOutputData().put("status", "FAILED");
        }

        return result;
    }
}
