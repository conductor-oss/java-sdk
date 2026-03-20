package exceptionhandling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for eh_finalize — finalizes the workflow after processing.
 *
 * Returns finalized=true upon completion.
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eh_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [eh_finalize] Finalizing...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalized", true);
        return result;
    }
}
