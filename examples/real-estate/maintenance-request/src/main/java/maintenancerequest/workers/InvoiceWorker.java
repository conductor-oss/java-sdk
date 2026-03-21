package maintenancerequest.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class InvoiceWorker implements Worker {
    @Override public String getTaskDefName() { return "mtr_invoice"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [mtr_invoice] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("totalCost", "351.50");
        result.getOutputData().put("priority", "high");
        return result;
    }
}
