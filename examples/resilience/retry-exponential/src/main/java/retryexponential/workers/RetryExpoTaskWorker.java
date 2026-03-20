package retryexponential.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs an API that returns 429 (Too Many Requests) for the first 2 calls,
 * then succeeds on the 3rd attempt.
 *
 * Conductor's EXPONENTIAL_BACKOFF retry logic doubles the wait time between
 * retries: 1s, 2s, 4s, 8s, ...
 */
public class RetryExpoTaskWorker implements Worker {

    private final AtomicInteger attemptCounter = new AtomicInteger(0);
    private final int failUntilAttempt;

    /**
     * Creates a worker that fails with FAILED status for the first N attempts,
     * performing a 429 response, then succeeds.
     *
     * @param failUntilAttempt number of attempts that should fail (0-based counting;
     *                         e.g., 2 means attempts 1 and 2 fail, attempt 3 succeeds)
     */
    public RetryExpoTaskWorker(int failUntilAttempt) {
        this.failUntilAttempt = failUntilAttempt;
    }

    /**
     * Default constructor: fails for the first 2 attempts, succeeds on the 3rd.
     */
    public RetryExpoTaskWorker() {
        this(2);
    }

    @Override
    public String getTaskDefName() {
        return "retry_expo_task";
    }

    @Override
    public TaskResult execute(Task task) {
        int attempt = attemptCounter.incrementAndGet();
        String apiUrl = getApiUrl(task);

        TaskResult result = new TaskResult(task);

        if (attempt <= failUntilAttempt) {
            System.out.println("  [retry_expo_task] Attempt " + attempt
                    + ": API returned 429 (deterministic. for " + apiUrl);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion(
                    "HTTP 429 Too Many Requests (attempt " + attempt + ")");
            result.getOutputData().put("attempts", attempt);
            result.getOutputData().put("error", "429 Too Many Requests");
            return result;
        }

        System.out.println("  [retry_expo_task] Attempt " + attempt
                + ": API returned 200 OK for " + apiUrl);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("attempts", attempt);
        result.getOutputData().put("data", Map.of("status", "ok"));
        return result;
    }

    /**
     * Resets the attempt counter. Useful for testing.
     */
    public void resetAttempts() {
        attemptCounter.set(0);
    }

    /**
     * Returns the current attempt count. Useful for testing.
     */
    public int getAttemptCount() {
        return attemptCounter.get();
    }

    private String getApiUrl(Task task) {
        Object apiUrlRaw = task.getInputData().get("apiUrl");
        if (apiUrlRaw == null) {
            return "https://api.example.com/data";
        }
        String apiUrl = String.valueOf(apiUrlRaw);
        if (apiUrl.isBlank()) {
            return "https://api.example.com/data";
        }
        return apiUrl;
    }
}
