package priceoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Updates the product price and computes the price change delta.
 */
public class UpdatePriceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prz_update_price";
    }

    @Override
    public TaskResult execute(Task task) {
        double oldPrice = toDouble(task.getInputData().get("currentPrice"), 0);
        double newPrice = toDouble(task.getInputData().get("newPrice"), 0);
        double change = Math.round((newPrice - oldPrice) * 100.0) / 100.0;

        System.out.println("  [update] Price updated: $" + oldPrice + " -> $" + newPrice
                + " (" + (change >= 0 ? "+" : "") + "$" + change + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("priceChange", change);
        result.getOutputData().put("updated", true);
        result.getOutputData().put("effectiveAt", "2026-01-15T10:00:00Z");
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
