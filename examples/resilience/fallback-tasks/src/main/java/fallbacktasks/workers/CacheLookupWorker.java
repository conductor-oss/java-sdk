package fallbacktasks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Cache lookup worker (fb_cache_lookup) -- last-resort fallback using cached data.
 *
 * Always succeeds. Returns:
 *   source = "cache"
 *   data   = "stale data from cache"
 *   stale  = true
 */
public class CacheLookupWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fb_cache_lookup";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("source", "cache");
        result.getOutputData().put("data", "stale data from cache");
        result.getOutputData().put("stale", true);
        return result;
    }
}
