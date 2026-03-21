package approvalcomments.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for ac_prepare_doc -- prepares a document for review.
 *
 * Returns { ready: true } to indicate the document is ready for human review.
 */
public class PrepareDocWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ac_prepare_doc";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ready", true);
        return result;
    }
}
