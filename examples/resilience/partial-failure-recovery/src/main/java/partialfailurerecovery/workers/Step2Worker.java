package partialfailurerecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker for pfr_step2 — second step that performs a transient failure.
 *
 * Fails on the first attempt, then succeeds on retry. This demonstrates
 * how Conductor can resume a workflow from the last good state using
 * the POST /workflow/{id}/retry API.
 *
 * Takes a "prev" input and returns { result: "s2-{prev}" } on success.
 */
public class Step2Worker implements Worker {

    private final AtomicInteger attemptCounter = new AtomicInteger(0);

    @Override
    public String getTaskDefName() {
        return "pfr_step2";
    }

    @Override
    public TaskResult execute(Task task) {
        String prev = "";
        Object prevInput = task.getInputData().get("prev");
        if (prevInput != null) {
            prev = prevInput.toString();
        }

        int attempt = attemptCounter.incrementAndGet();

        TaskResult result = new TaskResult(task);

        if (attempt == 1) {
            System.out.println("  [pfr_step2] Attempt " + attempt + ": Performing failure");
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Intentional transient failure on attempt " + attempt);
        } else {
            System.out.println("  [pfr_step2] Attempt " + attempt + ": Success (prev=" + prev + ")");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("result", "s2-" + prev);
        }

        return result;
    }

    /**
     * Resets the attempt counter. Used for testing.
     */
    public void reset() {
        attemptCounter.set(0);
    }
}
