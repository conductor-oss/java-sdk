package networkpartitions.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker for np_resilient_task -- demonstrates handling network partitions.
 *
 * Tracks attempt count using an AtomicInteger for thread-safe operation.
 * Each invocation increments the counter and returns the result with the
 * current attempt number. This performs a worker that reconnects after
 * a network partition and continues processing.
 *
 * Returns: { result: "done-{data}", attempt: attemptCount }
 */
public class NetworkPartitionWorker implements Worker {

    private final AtomicInteger attemptCounter = new AtomicInteger(0);

    @Override
    public String getTaskDefName() {
        return "np_resilient_task";
    }

    @Override
    public TaskResult execute(Task task) {
        int attempt = attemptCounter.incrementAndGet();

        String data = "";
        Object dataInput = task.getInputData().get("data");
        if (dataInput != null) {
            data = dataInput.toString();
        }

        System.out.println("  [np_resilient_task] Processing attempt " + attempt + " with data=" + data);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "done-" + data);
        result.getOutputData().put("attempt", attempt);

        return result;
    }

    /**
     * Returns the current attempt count.
     */
    public int getAttemptCount() {
        return attemptCounter.get();
    }

    /**
     * Resets the attempt counter to zero.
     */
    public void reset() {
        attemptCounter.set(0);
    }
}
