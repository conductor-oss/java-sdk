package workflowvariables.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Calculates the base price (subtotal) from a list of order items.
 * Each item has a name, price, and qty. Returns the subtotal and item count.
 */
public class CalcPriceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wv_calc_price";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) task.getInputData().get("items");
        if (items == null) {
            items = List.of();
        }

        double subtotal = 0.0;
        for (Map<String, Object> item : items) {
            double price = ((Number) item.get("price")).doubleValue();
            int qty = ((Number) item.get("qty")).intValue();
            subtotal += price * qty;
        }
        // Round to 2 decimal places
        subtotal = Math.round(subtotal * 100.0) / 100.0;

        System.out.println("  [price] " + items.size() + " items -> $" + subtotal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("subtotal", subtotal);
        result.getOutputData().put("itemCount", items.size());
        return result;
    }
}
