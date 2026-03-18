package understandingworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Calculates order totals from validated items.
 *
 * Input:  validatedItems (List of {name, price, qty, validated, inStock})
 * Output: subtotal (double), tax (double), total (double)
 */
public class CalculateTotalWorker implements Worker {

    private static final double TAX_RATE = 0.08;

    @Override
    public String getTaskDefName() {
        return "calculate_total";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> validatedItems =
                (List<Map<String, Object>>) task.getInputData().get("validatedItems");

        double subtotal = 0.0;
        if (validatedItems != null) {
            for (Map<String, Object> item : validatedItems) {
                double price = toDouble(item.get("price"));
                int qty = toInt(item.get("qty"));
                subtotal += price * qty;
            }
        }

        double tax = Math.round(subtotal * TAX_RATE * 100.0) / 100.0;
        double total = Math.round((subtotal + tax) * 100.0) / 100.0;
        subtotal = Math.round(subtotal * 100.0) / 100.0;

        System.out.println("  [calculate_total] Subtotal: $" + subtotal
                + ", Tax: $" + tax + ", Total: $" + total);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("subtotal", subtotal);
        result.getOutputData().put("tax", tax);
        result.getOutputData().put("total", total);
        return result;
    }

    private static double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) return Double.parseDouble((String) value);
        return 0.0;
    }

    private static int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) return Integer.parseInt((String) value);
        return 0;
    }
}
