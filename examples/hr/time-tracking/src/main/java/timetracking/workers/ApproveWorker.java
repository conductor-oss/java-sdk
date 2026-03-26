package timetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "ttk_approve"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [approve] Timesheet " + task.getInputData().get("timesheetId") + " approved by manager");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", true);
        result.getOutputData().put("approvedBy", "MGR-60");
        return result;
    }
}
