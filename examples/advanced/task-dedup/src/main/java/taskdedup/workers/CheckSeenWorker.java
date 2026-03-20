package taskdedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Checks if a hash has been seen before to determine duplicate status.
 * Input: hash, cacheEnabled
 * Output: status ("new" or "dup"), cachedResult
 */
public class CheckSeenWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tdd_check_seen";
    }

    @Override
    public TaskResult execute(Task task) {
        String hash = (String) task.getInputData().get("hash");
        if (hash == null) {
            hash = "unknown";
        }

        // first run is always "new"
        boolean isDuplicate = false;
        String status = isDuplicate ? "dup" : "new";

        System.out.println("  [check] Hash " + hash + ": " + status);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", status);
        result.getOutputData().put("cachedResult",
                isDuplicate ? Map.of("cached", true, "value", "previous_result") : null);
        return result;
    }
}
