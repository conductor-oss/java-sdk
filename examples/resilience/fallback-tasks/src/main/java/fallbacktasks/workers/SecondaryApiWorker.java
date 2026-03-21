package fallbacktasks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Secondary API worker (fb_secondary_api) -- fallback API data source.
 *
 * Always succeeds. Returns:
 *   source = "secondary"
 *   data   = "fallback data from secondary API"
 */
public class SecondaryApiWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fb_secondary_api";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("source", "secondary");
        result.getOutputData().put("data", "fallback data from secondary API");
        return result;
    }
}
