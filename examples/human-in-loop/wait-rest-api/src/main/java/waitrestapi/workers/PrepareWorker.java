package waitrestapi.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for wapi_prepare — prepares the workflow for the WAIT approval gate.
 *
 * Returns { ready: true } to indicate preparation is complete.
 */
public class PrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wapi_prepare";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [wapi_prepare] Preparing workflow for approval gate...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ready", true);

        return result;
    }
}
