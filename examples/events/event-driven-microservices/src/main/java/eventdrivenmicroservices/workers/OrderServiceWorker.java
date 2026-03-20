package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Creates an order from customer and items input.
 * Input: customerId, items (list of maps with name, price, qty)
 * Output: orderId ("ORD-DM-1001"), items, totalAmount, status ("created")
 */
public class OrderServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_order_service";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object itemsRaw = task.getInputData().get("items");
        List<Map<String, Object>> items;
        if (itemsRaw instanceof List) {
            items = (List<Map<String, Object>>) itemsRaw;
        } else {
            items = List.of();
        }

        double totalAmount = 0.0;
        for (Map<String, Object> item : items) {
            double price = toDouble(item.get("price"));
            int qty = toInt(item.get("qty"), 1);
            totalAmount += price * qty;
        }

        String orderId = "ORD-DM-1001";

        System.out.println("  [order-svc] Created order " + orderId + ": "
                + items.size() + " items, total $" + String.format("%.2f", totalAmount));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderId", orderId);
        result.getOutputData().put("items", items);
        result.getOutputData().put("totalAmount", totalAmount);
        result.getOutputData().put("status", "created");
        return result;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private int toInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
