package timetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "ttk_submit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Timesheet from " + task.getInputData().get("employeeId") + " for week ending " + task.getInputData().get("weekEnding"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("timesheetId", "TS-606");
        result.getOutputData().put("submitted", true);
        return result;
    }
}
