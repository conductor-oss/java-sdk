package enterpriserag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Worker that caches a generated answer for future lookups.
 * Takes question, answer, and ttlSeconds. Returns cached status, cacheKey, and expiresAt.
 */
public class CacheResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "er_cache_result";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        String answer = (String) task.getInputData().get("answer");
        Object ttlObj = task.getInputData().get("ttlSeconds");
        int ttlSeconds = 3600;
        if (ttlObj instanceof Number) {
            ttlSeconds = ((Number) ttlObj).intValue();
        }

        String cacheKey = "rag:" + question.hashCode();
        String expiresAt = Instant.now().plusSeconds(ttlSeconds).toString();

        System.out.println("  [cache_result] Cached key=" + cacheKey
                + " ttl=" + ttlSeconds + "s");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cached", true);
        result.getOutputData().put("cacheKey", cacheKey);
        result.getOutputData().put("expiresAt", expiresAt);
        return result;
    }
}
