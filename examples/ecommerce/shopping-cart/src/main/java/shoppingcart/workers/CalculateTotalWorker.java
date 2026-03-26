package shoppingcart.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates the subtotal for items in the cart.
 * Input: cartId, items (list of cart items with price and quantity)
 * Output: subtotal, taxAmount, itemCount
 */
public class CalculateTotalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cart_calculate_total";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String cartId = (String) task.getInputData().get("cartId");
        if (cartId == null) cartId = "UNKNOWN";
        List<Map<String, Object>> items = (List<Map<String, Object>>) task.getInputData().get("items");
        if (items == null) items = List.of();

        double subtotal = 0.0;
        for (Map<String, Object> item : items) {
            double price = 0;
            Object priceObj = item.get("price");
            if (priceObj instanceof Number) price = ((Number) priceObj).doubleValue();

            int qty = 1;
            Object qtyObj = item.get("quantity");
            if (qtyObj instanceof Number) qty = ((Number) qtyObj).intValue();

            subtotal += price * qty;
        }

        double taxRate = 0.08;
        double taxAmount = Math.round(subtotal * taxRate * 100.0) / 100.0;

        System.out.println("  [total] Cart " + cartId + ": subtotal=$" + subtotal + " tax=$" + taxAmount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("subtotal", subtotal);
        output.put("taxAmount", taxAmount);
        output.put("itemCount", items.size());
        result.setOutputData(output);
        return result;
    }
}
