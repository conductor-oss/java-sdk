package foodordering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PayWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fod_pay";
    }

    @Override
    public TaskResult execute(Task task) {
        Object total = task.getInputData().get("total");
        String orderId = (String) task.getInputData().get("orderId");
        System.out.println("  [pay] Processing $" + total + " for order " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("paid", true);
        result.addOutputData("transactionId", "TXN-8821");
        return result;
    }
}
