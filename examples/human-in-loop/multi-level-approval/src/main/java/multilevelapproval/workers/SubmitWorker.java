package multilevelapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for mla_submit task -- submits a request for multi-level approval.
 *
 * Reads input and returns { submitted: true } to indicate the request
 * has been submitted into the approval chain.
 */
public class SubmitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mla_submit";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [mla_submit] Processing submission...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("submitted", true);

        return result;
    }
}
