package taskdedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Returns a previously cached result for a duplicate task.
 * Input: hash, cachedResult
 * Output: result, fromCache
 */
public class ReturnCachedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tdd_return_cached";
    }

    @Override
    public TaskResult execute(Task task) {
        String hash = (String) task.getInputData().get("hash");
        if (hash == null) {
            hash = "unknown";
        }
        Object cachedResult = task.getInputData().get("cachedResult");

        System.out.println("  [cached] Returning cached result for hash " + hash);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", cachedResult);
        result.getOutputData().put("fromCache", true);
        return result;
    }
}
