package toolusecaching.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Returns a previously cached result directly.
 *
 * Input fields: cachedResult, cacheKey, cachedAt.
 * Output fields: result, fromCache, cacheAge.
 */
public class ReturnCachedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "uc_return_cached";
    }

    @Override
    public TaskResult execute(Task task) {
        Object cachedResult = task.getInputData().get("cachedResult");
        String cacheKey = (String) task.getInputData().get("cacheKey");
        String cachedAt = (String) task.getInputData().get("cachedAt");

        if (cacheKey == null) {
            cacheKey = "unknown";
        }
        if (cachedAt == null) {
            cachedAt = "unknown";
        }

        System.out.println("  [uc_return_cached] Returning cached result for key: " + cacheKey);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", cachedResult);
        result.getOutputData().put("fromCache", true);
        result.getOutputData().put("cacheAge", "45s");
        return result;
    }
}
