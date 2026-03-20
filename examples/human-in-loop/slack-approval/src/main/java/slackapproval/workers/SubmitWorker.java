package slackapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sa_submit — handles the initial submission step.
 *
 * Accepts workflow input and marks the submission as received.
 * Returns { submitted: true }.
 */
public class SubmitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sa_submit";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sa_submit] Processing submission...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("submitted", true);
        return result;
    }
}
