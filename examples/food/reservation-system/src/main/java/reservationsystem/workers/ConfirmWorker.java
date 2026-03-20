package reservationsystem.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "rsv_confirm"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [confirm] Confirmation sent to " + task.getInputData().get("guestName"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("confirmed", true);
        result.addOutputData("confirmationCode", "CF-9922");
        return result;
    }
}
