package hotelbooking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReminderWorker implements Worker {
    @Override public String getTaskDefName() { return "htl_reminder"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [reminder] Check-in reminder scheduled for " + task.getInputData().get("travelerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reminderSet", true); r.getOutputData().put("reminderDate", "2024-04-14"); return r;
    }
}
