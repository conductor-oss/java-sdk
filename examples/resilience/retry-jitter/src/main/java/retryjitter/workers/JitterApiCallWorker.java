package retryjitter.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker for jitter_api_call — adds a deterministic jitter delay before processing
 * to avoid the thundering herd problem.
 *
 * Jitter is computed deterministically from the endpoint's hashCode:
 *   jitterMs = Math.abs(endpoint.hashCode() % 500)
 *
 * This produces a fixed 0–499ms value per endpoint, making the worker fully testable
 * without randomness.
 *
 * Uses an AtomicInteger to count calls across invocations.
 */
public class JitterApiCallWorker implements Worker {

    private final AtomicInteger callCount = new AtomicInteger(0);

    @Override
    public String getTaskDefName() {
        return "jitter_api_call";
    }

    @Override
    public TaskResult execute(Task task) {
        String endpoint = "default";
        Object endpointInput = task.getInputData().get("endpoint");
        if (endpointInput instanceof String) {
            endpoint = (String) endpointInput;
        }

        int attempt = callCount.incrementAndGet();
        int jitterMs = Math.abs(endpoint.hashCode() % 500);

        System.out.println("  [jitter_api_call] Attempt " + attempt
                + " | endpoint=" + endpoint
                + " | jitterMs=" + jitterMs);

        // In production, sleep for jitterMs to spread out retries.
        // In unit tests, the caller verifies the computed jitter without sleeping.
        try {
            Thread.sleep(jitterMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "ok");
        result.getOutputData().put("jitterMs", jitterMs);
        result.getOutputData().put("attempt", attempt);

        return result;
    }

    /**
     * Returns the current call count (useful for testing).
     */
    public int getCallCount() {
        return callCount.get();
    }
}
