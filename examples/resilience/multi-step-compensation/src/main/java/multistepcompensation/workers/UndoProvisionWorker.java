package multistepcompensation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for msc_undo_provision — undoes resource provisioning.
 */
public class UndoProvisionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "msc_undo_provision";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [msc_undo_provision] Undoing resource provisioning...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("undone", true);

        System.out.println("  [msc_undo_provision] Resource provisioning undone");
        return result;
    }
}
