package llmcaching;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker 1: cache_hash_prompt
 * Takes prompt and model, creates a cache key by concatenating model+":"+prompt,
 * replacing whitespace with "_", and truncating to 64 chars.
 */
public class CacheHashPromptWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cache_hash_prompt";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().get("prompt");
        String model = (String) task.getInputData().get("model");

        String raw = model + ":" + prompt;
        String normalized = raw.replaceAll("\\s+", "_");
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("cacheKey", normalized);
        return result;
    }
}
