package toolusecaching.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Stores the tool execution result in the cache.
 *
 * Input fields: cacheKey, result, ttlSeconds.
 * Output fields: storedResult, cached, cacheKey, expiresAt, ttlSeconds.
 */
public class CacheResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "uc_cache_result";
    }

    @Override
    public TaskResult execute(Task task) {
        String cacheKey = (String) task.getInputData().get("cacheKey");
        if (cacheKey == null || cacheKey.isBlank()) {
            cacheKey = "unknown";
        }

        Object inputResult = task.getInputData().get("result");

        Object ttlObj = task.getInputData().get("ttlSeconds");
        int ttlSeconds = 300;
        if (ttlObj instanceof Number) {
            ttlSeconds = ((Number) ttlObj).intValue();
        }

        System.out.println("  [uc_cache_result] Caching result for key: " + cacheKey);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("storedResult", inputResult);
        result.getOutputData().put("cached", true);
        result.getOutputData().put("cacheKey", cacheKey);
        result.getOutputData().put("expiresAt", "2026-03-08T10:05:00Z");
        result.getOutputData().put("ttlSeconds", ttlSeconds);
        return result;
    }
}
