package legalbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectWorker implements Worker {
    @Override public String getTaskDefName() { return "lgb_collect"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Tracking payment for invoice " + task.getInputData().get("invoiceId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("paymentStatus", "paid");
        result.getOutputData().put("paymentDate", java.time.LocalDate.now().toString());
        return result;
    }
}
