package responsetimeout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker for the resp_timeout_task.
 *
 * Responds quickly within the 3-second response timeout.
 * Tracks the attempt number across invocations.
 */
public class RespTimeoutWorker implements Worker {

    private final AtomicInteger attempt = new AtomicInteger(0);

    @Override
    public String getTaskDefName() {
        return "resp_timeout_task";
    }

    @Override
    public TaskResult execute(Task task) {
        int currentAttempt = attempt.incrementAndGet();

        String mode = (String) task.getInputData().get("mode");
        if (mode == null || mode.isBlank()) {
            mode = "fast";
        }

        System.out.println("  [resp_timeout_task] Attempt " + currentAttempt
                + " — processing quickly (mode=" + mode + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "fast-response");
        result.getOutputData().put("attempt", currentAttempt);
        return result;
    }

    /**
     * Returns the current attempt count (useful for testing).
     */
    public int getAttemptCount() {
        return attempt.get();
    }

    /**
     * Resets the attempt counter (useful for testing).
     */
    public void resetAttemptCount() {
        attempt.set(0);
    }
}
