package eventdedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks whether a given hash has been seen before (deterministic.lookup).
 * Always returns "duplicate" to perform a previously-seen event.
 *
 * Input:  {hash}
 * Output: {status: "duplicate", checkedAt: "2026-01-15T10:00:00Z"}
 */
public class CheckSeenWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dd_check_seen";
    }

    @Override
    public TaskResult execute(Task task) {
        String hash = (String) task.getInputData().get("hash");

        System.out.println("  [dd_check_seen] Checking if hash was seen: " + hash);

        // hash was already seen
        String status = "duplicate";
        String checkedAt = "2026-01-15T10:00:00Z";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", status);
        result.getOutputData().put("checkedAt", checkedAt);
        return result;
    }
}
