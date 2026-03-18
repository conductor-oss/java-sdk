package choreographyvsorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class PlaceOrderWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "cvo_place_order";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String orderId = (String) task.getInputData().getOrDefault("orderId", null);
        String customerId = (String) task.getInputData().getOrDefault("customerId", null);
        Object itemsObj = task.getInputData().get("items");

        // Validate required fields
        if (orderId == null || orderId.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Missing required field: orderId");
            return r;
        }
        if (customerId == null || customerId.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Missing required field: customerId");
            return r;
        }
        if (itemsObj == null) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Missing required field: items");
            return r;
        }

        // Parse items and compute total
        List<?> items;
        if (itemsObj instanceof List) {
            items = (List<?>) itemsObj;
        } else {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("items must be a list");
            return r;
        }

        if (items.isEmpty()) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Order must contain at least one item");
            return r;
        }

        // Compute total: items can be strings (flat price) or maps with "price" and "quantity"
        double total = 0.0;
        int totalQuantity = 0;
        for (Object item : items) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) item;
                double price = toDouble(itemMap.getOrDefault("price", 10.0));
                int qty = toInt(itemMap.getOrDefault("quantity", 1));
                total += price * qty;
                totalQuantity += qty;
            } else {
                // Simple item name with default price
                total += 29.99;
                totalQuantity += 1;
            }
        }

        total = Math.round(total * 100.0) / 100.0;

        System.out.println("  [order] Order " + orderId + " placed for customer " + customerId
                + " (" + items.size() + " line items, " + totalQuantity + " units, total=$" + total + ")");

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("orderId", orderId);
        r.getOutputData().put("customerId", customerId);
        r.getOutputData().put("total", total);
        r.getOutputData().put("itemCount", items.size());
        r.getOutputData().put("totalQuantity", totalQuantity);
        r.getOutputData().put("status", "placed");
        r.getOutputData().put("placedAt", Instant.now().toString());
        return r;
    }

    private static double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0.0; }
    }

    private static int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception e) { return 1; }
    }
}
