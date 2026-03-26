package dashboarddata.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Caches the assembled dashboard for fast retrieval.
 * Input: dashboardId, widgets (list), refreshInterval
 * Output: cacheKey, ttl, ready, cachedAt
 */
public class CacheDashboardWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dh_cache_dashboard";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String dashId = (String) task.getInputData().getOrDefault("dashboardId", "unknown");
        List<Map<String, Object>> widgets =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("widgets", List.of());
        Object intervalObj = task.getInputData().getOrDefault("refreshInterval", 300);
        int interval = (intervalObj instanceof Number) ? ((Number) intervalObj).intValue() : 300;

        String cacheKey = "dashboard:" + dashId + ":" + System.currentTimeMillis();

        System.out.println("  [cache] Cached " + widgets.size() + " widgets at " + cacheKey + ", TTL: " + interval + "s");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cacheKey", cacheKey);
        result.getOutputData().put("ttl", interval + "s");
        result.getOutputData().put("ready", true);
        result.getOutputData().put("cachedAt", "2024-03-15T15:30:00Z");
        return result;
    }
}
