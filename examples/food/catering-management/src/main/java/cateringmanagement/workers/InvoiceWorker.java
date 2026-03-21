package cateringmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class InvoiceWorker implements Worker {
    @Override public String getTaskDefName() { return "cat_invoice"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [invoice] Invoicing " + task.getInputData().get("clientName") + ": $" + task.getInputData().get("total"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("invoice", Map.of(
            "client", task.getInputData().getOrDefault("clientName", "Acme Corp"),
            "total", task.getInputData().getOrDefault("total", 4500),
            "invoiceId", "INV-740", "status", "SENT"));
        return result;
    }
}
