package multilevelapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for mla_finalize task -- finalizes a fully-approved request.
 *
 * Called after all three approval levels (Manager, Director, VP) have
 * approved. Returns { finalized: true }.
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mla_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [mla_finalize] Finalizing approved request...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalized", true);

        return result;
    }
}
