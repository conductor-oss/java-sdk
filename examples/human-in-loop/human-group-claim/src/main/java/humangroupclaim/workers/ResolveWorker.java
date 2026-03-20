package humangroupclaim.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for hgc_resolve — resolves the ticket and returns closed=true.
 *
 * This worker handles the final resolution step of the group assignment
 * workflow, marking the ticket as closed after human review.
 */
public class ResolveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hgc_resolve";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("closed", true);
        return result;
    }
}
