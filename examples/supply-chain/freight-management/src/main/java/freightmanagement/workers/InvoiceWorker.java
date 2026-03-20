package freightmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class InvoiceWorker implements Worker {
    @Override public String getTaskDefName() { return "frm_invoice"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [invoice] Generated invoice for $" + task.getInputData().get("rate"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("invoiceId", "INV-667-001"); r.getOutputData().put("amount", task.getInputData().get("rate")); return r;
    }
}
