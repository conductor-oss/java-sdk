package billingtelecom.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class InvoiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "btl_invoice";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [invoice] Invoice generated — total: $54.50");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("invoiceId", "INV-billing-telecom-001");
        result.getOutputData().put("totalAmount", 54.5);
        result.getOutputData().put("dueDate", "2024-04-01");
        return result;
    }
}
