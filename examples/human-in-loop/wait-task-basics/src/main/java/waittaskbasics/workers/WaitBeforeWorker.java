package waittaskbasics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for wait_before — prepares data before the WAIT task.
 *
 * Returns { prepared: true } to indicate the workflow is ready
 * to pause at the WAIT task and await external completion.
 */
public class WaitBeforeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wait_before";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [wait_before] Preparing for wait...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("prepared", true);

        return result;
    }
}
