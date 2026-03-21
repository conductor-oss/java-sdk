package webinarregistration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "wbr_confirm"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [confirm] Confirmation email sent to " + task.getInputData().get("email"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("confirmed", true);
        result.getOutputData().put("calendarInvite", true);
        return result;
    }
}
