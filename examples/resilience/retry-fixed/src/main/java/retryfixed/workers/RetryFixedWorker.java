package retryfixed.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker for retry_fixed_task — performs transient failures.
 *
 * Uses an attempt counter keyed by workflow ID. Fails the first {@code failCount}
 * times, then succeeds. Returns { attempts: N, result: "success" } on success.
 *
 * The task definition is configured with:
 * - retryCount: 3
 * - retryLogic: FIXED
 * - retryDelaySeconds: 1
 */
public class RetryFixedWorker implements Worker {

    private final ConcurrentHashMap<String, AtomicInteger> attemptCounters = new ConcurrentHashMap<>();

    @Override
    public String getTaskDefName() {
        return "retry_fixed_task";
    }

    @Override
    public TaskResult execute(Task task) {
        int failCount = 0;
        Object failCountInput = task.getInputData().get("failCount");
        if (failCountInput instanceof Number) {
            failCount = ((Number) failCountInput).intValue();
        }

        String workflowId = task.getWorkflowInstanceId() != null
                ? task.getWorkflowInstanceId()
                : "default";
        AtomicInteger counter = attemptCounters.computeIfAbsent(workflowId, k -> new AtomicInteger(0));
        int attempt = counter.incrementAndGet();

        System.out.println("  [retry_fixed_task] Attempt " + attempt + " (failCount=" + failCount + ")");

        TaskResult result = new TaskResult(task);

        if (attempt <= failCount) {
            System.out.println("  [retry_fixed_task] Performing failure (attempt " + attempt + " of " + failCount + " failures)");
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("attempts", attempt);
            result.getOutputData().put("error", "Intentional failure on attempt " + attempt);
        } else {
            System.out.println("  [retry_fixed_task] Success on attempt " + attempt);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("attempts", attempt);
            result.getOutputData().put("result", "success");
            attemptCounters.remove(workflowId);
        }

        return result;
    }
}
