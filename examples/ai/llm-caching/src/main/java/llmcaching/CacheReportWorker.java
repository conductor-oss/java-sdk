package llmcaching;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker 3: cache_report
 * Takes cacheHit, response, latencyMs. Outputs a saved message.
 */
public class CacheReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cache_report";
    }

    @Override
    public TaskResult execute(Task task) {
        Object cacheHitObj = task.getInputData().get("cacheHit");
        boolean cacheHit = Boolean.TRUE.equals(cacheHitObj);

        String saved = cacheHit
                ? "Yes — saved ~$0.02"
                : "No — first request";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("saved", saved);
        return result;
    }
}
