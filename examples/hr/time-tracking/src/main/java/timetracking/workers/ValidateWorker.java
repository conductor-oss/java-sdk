package timetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "ttk_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Timesheet " + task.getInputData().get("timesheetId") + ": 40 hours, no anomalies");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("totalHours", 40);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("overtime", 0);
        return result;
    }
}
