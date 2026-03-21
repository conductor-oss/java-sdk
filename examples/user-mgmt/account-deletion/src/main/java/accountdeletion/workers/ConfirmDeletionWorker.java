package accountdeletion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConfirmDeletionWorker implements Worker {
    @Override public String getTaskDefName() { return "acd_confirm"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [confirm] Deletion confirmation sent");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("confirmationSent", true);
        result.getOutputData().put("gdprCompliant", true);
        return result;
    }
}
