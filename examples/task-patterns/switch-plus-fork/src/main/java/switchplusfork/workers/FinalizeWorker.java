package switchplusfork.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Common finalizer that runs after both the batch and single-item paths.
 * Returns done: true to signal completion.
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sf_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sf_finalize] Finalizing workflow");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("done", true);
        return result;
    }
}
