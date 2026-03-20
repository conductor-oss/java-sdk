package freightmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReconcileWorker implements Worker {
    @Override public String getTaskDefName() { return "frm_reconcile"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [reconcile] " + task.getInputData().get("invoiceId") + " matched to " + task.getInputData().get("bookingId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reconciled", true); r.getOutputData().put("discrepancy", 0); return r;
    }
}
