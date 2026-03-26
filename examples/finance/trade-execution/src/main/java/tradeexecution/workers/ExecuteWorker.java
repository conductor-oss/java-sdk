package tradeexecution.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Executes the trade on the routed exchange.
 */
public class ExecuteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "trd_execute";
    }

    @Override
    public TaskResult execute(Task task) {
        double fillPrice = 175.42;
        Object qtyObj = task.getInputData().get("quantity");
        int quantity = 0;
        if (qtyObj instanceof Number) {
            quantity = ((Number) qtyObj).intValue();
        } else if (qtyObj instanceof String) {
            try { quantity = Integer.parseInt((String) qtyObj); } catch (NumberFormatException ignored) {}
        }
        double totalValue = Math.round(fillPrice * quantity * 100.0) / 100.0;

        String side = (String) task.getInputData().get("side");
        String symbol = (String) task.getInputData().get("symbol");
        String exchange = (String) task.getInputData().get("exchange");

        System.out.println("  [execute] Filled " + side + " " + quantity + " " + symbol + " @ $" + fillPrice + " on " + exchange);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("executionId", "EXEC-TRD-44201");
        result.getOutputData().put("fillPrice", fillPrice);
        result.getOutputData().put("totalValue", totalValue);
        result.getOutputData().put("executedAt", Instant.now().toString());
        return result;
    }
}
