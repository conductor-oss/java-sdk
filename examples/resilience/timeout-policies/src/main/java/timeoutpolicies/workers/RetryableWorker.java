package timeoutpolicies.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for the tp_retryable task.
 *
 * The task definition uses timeoutPolicy: RETRY.
 * If this worker does not respond within responseTimeoutSeconds,
 * the task is automatically retried (up to retryCount times).
 */
public class RetryableWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tp_retryable";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [tp_retryable] Processing -- retryable service call");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "retried-done");
        return result;
    }
}
