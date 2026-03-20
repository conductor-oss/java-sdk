package fallbacktasks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Primary API worker (fb_primary_api) -- calls the primary data source.
 *
 * Input:
 *   available (boolean) -- whether the API is reachable
 *
 * Output when available=true:
 *   apiStatus = "ok"
 *   data      = "primary data from main API"
 *   source    = "primary"
 *
 * Output when available=false:
 *   apiStatus = "unavailable"
 *
 * Always completes (never FAILED). The workflow uses a SWITCH on apiStatus
 * to decide whether to invoke a fallback path.
 */
public class PrimaryApiWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fb_primary_api";
    }

    @Override
    public TaskResult execute(Task task) {
        boolean available = true;
        Object availableInput = task.getInputData().get("available");
        if (availableInput instanceof Boolean) {
            available = (Boolean) availableInput;
        } else if (availableInput instanceof String) {
            available = Boolean.parseBoolean((String) availableInput);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (available) {
            result.getOutputData().put("apiStatus", "ok");
            result.getOutputData().put("data", "primary data from main API");
            result.getOutputData().put("source", "primary");
        } else {
            result.getOutputData().put("apiStatus", "unavailable");
        }

        return result;
    }
}
