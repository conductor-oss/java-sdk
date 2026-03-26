package toolusecaching.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Checks the cache for a previously computed tool result.
 *
 * Computes a cacheKey from toolName + toolArgs and performs a cache MISS.
 * Output fields: cacheStatus, cacheKey, cachedResult, cachedAt, reason.
 */
public class CheckCacheWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "uc_check_cache";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String toolName = (String) task.getInputData().get("toolName");
        if (toolName == null || toolName.isBlank()) {
            toolName = "unknown_tool";
        }

        Map<String, Object> toolArgs = (Map<String, Object>) task.getInputData().get("toolArgs");
        if (toolArgs == null) {
            toolArgs = Map.of();
        }

        Object cacheTtlObj = task.getInputData().get("cacheTtlSeconds");
        int cacheTtlSeconds = 300;
        if (cacheTtlObj instanceof Number) {
            cacheTtlSeconds = ((Number) cacheTtlObj).intValue();
        }

        System.out.println("  [uc_check_cache] Checking cache for tool: " + toolName);

        // Compute cache key from toolName + sorted toolArgs
        String cacheKey = toolName + ":" + toolArgs.toString();

        // Perform a cache MISS
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cacheStatus", "miss");
        result.getOutputData().put("cacheKey", cacheKey);
        result.getOutputData().put("cachedResult", null);
        result.getOutputData().put("cachedAt", null);
        result.getOutputData().put("reason", "No cache entry exists for this key");
        return result;
    }
}
