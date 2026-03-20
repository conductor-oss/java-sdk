package multistepcompensation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for msc_undo_account — undoes account creation.
 */
public class UndoAccountWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "msc_undo_account";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [msc_undo_account] Undoing account creation...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("undone", true);

        System.out.println("  [msc_undo_account] Account creation undone");
        return result;
    }
}
