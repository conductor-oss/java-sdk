package humangroupclaim.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for hgc_intake — processes intake and returns queued=true.
 *
 * This worker handles the initial intake step of the group assignment
 * workflow, marking the ticket as queued for group processing.
 */
public class IntakeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hgc_intake";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("queued", true);
        return result;
    }
}
