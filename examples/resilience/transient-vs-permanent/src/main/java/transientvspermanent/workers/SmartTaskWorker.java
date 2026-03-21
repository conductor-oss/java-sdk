package transientvspermanent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Smart task worker (tvp_smart_task) that classifies errors as transient or permanent.
 *
 * Behavior based on the "errorType" input parameter:
 * - "transient": first 2 attempts return FAILED (Conductor retries), attempt 3+ returns COMPLETED
 * - "permanent": returns FAILED_WITH_TERMINAL_ERROR immediately (no retries)
 * - default/none: returns COMPLETED immediately
 *
 * The task definition is configured with:
 * - retryCount: 3
 * - retryLogic: FIXED
 * - retryDelaySeconds: 1
 */
public class SmartTaskWorker implements Worker {

    private final AtomicInteger attemptCounter = new AtomicInteger(0);

    @Override
    public String getTaskDefName() {
        return "tvp_smart_task";
    }

    @Override
    public TaskResult execute(Task task) {
        String errorType = null;
        Object errorTypeInput = task.getInputData().get("errorType");
        if (errorTypeInput instanceof String) {
            errorType = (String) errorTypeInput;
        }

        int attempt = attemptCounter.incrementAndGet();

        System.out.println("  [tvp_smart_task] Attempt " + attempt + " (errorType=" + errorType + ")");

        TaskResult result = new TaskResult(task);

        if ("permanent".equals(errorType)) {
            System.out.println("  [tvp_smart_task] Permanent error detected — failing with terminal error");
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.getOutputData().put("attempt", attempt);
            result.getOutputData().put("errorType", "permanent");
            result.getOutputData().put("error", "Permanent error — no retries");
        } else if ("transient".equals(errorType)) {
            if (attempt < 3) {
                System.out.println("  [tvp_smart_task] Transient error on attempt " + attempt + " — will retry");
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("attempt", attempt);
                result.getOutputData().put("errorType", "transient");
                result.getOutputData().put("error", "Transient error on attempt " + attempt);
            } else {
                System.out.println("  [tvp_smart_task] Transient error resolved on attempt " + attempt);
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("attempt", attempt);
                result.getOutputData().put("errorType", "transient");
                result.getOutputData().put("result", "success after transient errors");
            }
        } else {
            System.out.println("  [tvp_smart_task] No error — completing immediately");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("attempt", attempt);
            result.getOutputData().put("result", "success");
        }

        return result;
    }

    /**
     * Resets the attempt counter. Useful for testing.
     */
    public void reset() {
        attemptCounter.set(0);
    }
}
