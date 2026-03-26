package redisintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Manages cache TTL and memory.
 * Input: connectionId, key
 * Output: ttl, memoryUsage, encoding
 */
public class CacheMgmtWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "red_cache_mgmt";
    }

    @Override
    public TaskResult execute(Task task) {
        String key = (String) task.getInputData().get("key");
        int ttl = 3600;
        System.out.println("  [cache] Set TTL for " + key + ": " + ttl + "s, memory: 256 bytes");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ttl", ttl);
        result.getOutputData().put("memoryUsage", 256);
        result.getOutputData().put("encoding", "embstr");
        return result;
    }
}
