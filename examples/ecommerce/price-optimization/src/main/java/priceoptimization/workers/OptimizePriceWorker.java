package priceoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Optimizes the product price based on current price and demand score.
 * Formula: newPrice = currentPrice * (1 + (demandScore - 0.5) * 0.1)
 */
public class OptimizePriceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prz_optimize_price";
    }

    @Override
    public TaskResult execute(Task task) {
        double currentPrice = toDouble(task.getInputData().get("currentPrice"), 85.00);
        double demandScore = toDouble(task.getInputData().get("demandScore"), 0.5);

        double newPrice = Math.round(currentPrice * (1 + (demandScore - 0.5) * 0.1) * 100.0) / 100.0;
        double adjustmentPercent = Math.round(((newPrice - currentPrice) / currentPrice) * 10000.0) / 100.0;

        System.out.println("  [optimize] Current: $" + currentPrice + ", Optimized: $" + newPrice);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("newPrice", newPrice);
        result.getOutputData().put("adjustmentPercent", adjustmentPercent);
        result.getOutputData().put("confidence", 0.87);
        return result;
    }

    private double toDouble(Object value, double defaultValue) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble((String) value); } catch (NumberFormatException e) { /* ignore */ }
        }
        return defaultValue;
    }
}
