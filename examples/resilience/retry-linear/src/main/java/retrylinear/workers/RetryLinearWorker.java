package retrylinear.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker that performs a service that is unavailable for the first 3 attempts
 * and succeeds on the 4th attempt, demonstrating LINEAR_BACKOFF retry behavior.
 *
 * The delay between retries increases linearly: delay * attempt
 * (1s, 2s, 3s, 4s with retryDelaySeconds=1).
 */
public class RetryLinearWorker implements Worker {

    private final AtomicInteger attemptCounter = new AtomicInteger(0);

    @Override
    public String getTaskDefName() {
        return "retry_linear_task";
    }

    @Override
    public TaskResult execute(Task task) {
        String service = (String) task.getInputData().get("service");
        if (service == null || service.isBlank()) {
            service = "default-service";
        }

        int attempt = attemptCounter.incrementAndGet();

        System.out.println("  [retry_linear_task] Attempt " + attempt + " for service: " + service);

        if (attempt < 4) {
            System.out.println("  [retry_linear_task] Service unavailable (attempt " + attempt + "/4)");
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Service unavailable (attempt " + attempt + ")");
            result.getOutputData().put("attempts", attempt);
            result.getOutputData().put("serviceStatus", "unavailable");
            return result;
        }

        System.out.println("  [retry_linear_task] Service healthy, returning success");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("attempts", attempt);
        result.getOutputData().put("serviceStatus", "healthy");
        return result;
    }

    /**
     * Returns the current attempt count (for testing purposes).
     */
    public int getAttemptCount() {
        return attemptCounter.get();
    }

    /**
     * Resets the attempt counter (for testing purposes).
     */
    public void resetAttemptCounter() {
        attemptCounter.set(0);
    }
}
