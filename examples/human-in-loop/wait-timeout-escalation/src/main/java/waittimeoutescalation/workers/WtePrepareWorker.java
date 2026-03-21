package waittimeoutescalation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for wte_prepare — prepares the request before the WAIT task.
 *
 * Returns { ready: true } to indicate the system is ready for human input.
 */
public class WtePrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wte_prepare";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [wte_prepare] Preparing for wait...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ready", true);

        return result;
    }
}
