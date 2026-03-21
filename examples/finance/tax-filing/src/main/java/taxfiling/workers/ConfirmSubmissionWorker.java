package taxfiling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ConfirmSubmissionWorker implements Worker {
    @Override public String getTaskDefName() { return "txf_confirm_submission"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [confirm] Filing " + task.getInputData().get("filingId") + " confirmed: " + task.getInputData().get("confirmationNumber"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("confirmed", true); r.getOutputData().put("expectedRefundDate", "2026-04-15");
        r.getOutputData().put("receiptId", "RCP-503-2026");
        return r;
    }
}
