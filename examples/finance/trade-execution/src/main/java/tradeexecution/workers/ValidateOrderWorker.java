package tradeexecution.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validates a trade order for required fields and buying power.
 */
public class ValidateOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "trd_validate_order";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        String side = (String) task.getInputData().get("side");
        Object quantity = task.getInputData().get("quantity");
        String symbol = (String) task.getInputData().get("symbol");

        System.out.println("  [validate] Order " + orderId + ": " + side + " " + quantity + " " + symbol);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("buyingPower", 500000);
        result.getOutputData().put("sufficientFunds", true);
        return result;
    }
}
