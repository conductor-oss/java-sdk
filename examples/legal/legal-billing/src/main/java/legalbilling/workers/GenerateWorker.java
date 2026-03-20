package legalbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateWorker implements Worker {
    @Override public String getTaskDefName() { return "lgb_generate"; }

    @Override public TaskResult execute(Task task) {
        String clientId = (String) task.getInputData().get("clientId");
        System.out.println("  [generate] Generating invoice for client " + clientId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("invoiceId", "INV-" + System.currentTimeMillis());
        result.getOutputData().put("totalAmount", 3250.00);
        result.getOutputData().put("currency", "USD");
        return result;
    }
}
