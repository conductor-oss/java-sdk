package taskdedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a new (non-duplicate) task and caches the result for future dedup.
 * Input: payload, hash
 * Output: result, cachedForFuture, hash
 */
public class ExecuteNewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tdd_execute_new";
    }

    @Override
    public TaskResult execute(Task task) {
        String hash = (String) task.getInputData().get("hash");
        if (hash == null) {
            hash = "unknown";
        }

        System.out.println("  [execute] Processing new task (hash: " + hash + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "processed_successfully");
        result.getOutputData().put("cachedForFuture", true);
        result.getOutputData().put("hash", hash);
        return result;
    }
}
