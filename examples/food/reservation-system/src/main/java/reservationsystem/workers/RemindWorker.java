package reservationsystem.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RemindWorker implements Worker {
    @Override public String getTaskDefName() { return "rsv_remind"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [remind] Reminder sent for reservation " + task.getInputData().get("reservationId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("reminded", true);
        result.addOutputData("channel", "sms");
        return result;
    }
}
