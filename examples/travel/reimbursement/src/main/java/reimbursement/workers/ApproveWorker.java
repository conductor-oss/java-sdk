package reimbursement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "rmb_approve"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [approve] Claim " + task.getInputData().get("claimId") + " approved: $" + task.getInputData().get("amount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("approved", true); r.getOutputData().put("approvedBy", "MGR-100"); return r;
    }
}
