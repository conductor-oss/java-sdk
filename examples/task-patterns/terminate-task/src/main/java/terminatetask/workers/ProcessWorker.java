package terminatetask.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a validated order. Only reached if validation passed.
 *
 * Takes orderId and amount, returns processedAmount.
 */
public class ProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "term_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        Number amountNum = (Number) task.getInputData().get("amount");
        double amount = amountNum != null ? amountNum.doubleValue() : 0;

        System.out.println("  [process] Processing order " + orderId + ": $" + amount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedAmount", amount);
        return result;
    }
}
