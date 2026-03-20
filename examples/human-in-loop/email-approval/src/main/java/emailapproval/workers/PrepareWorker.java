package emailapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for ea_prepare — prepares the approval request.
 *
 * Reads the input and marks the request as ready for processing.
 * Returns { ready: true }.
 */
public class PrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ea_prepare";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ready", true);
        return result;
    }
}
