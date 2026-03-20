package reimbursement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "rmb_submit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Reimbursement claim: " + task.getInputData().get("employeeId") + ", $" + task.getInputData().get("amount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("claimId", "RMB-reimbursement"); r.getOutputData().put("submitted", true); return r;
    }
}
