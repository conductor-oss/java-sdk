package webhooktrigger.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Transforms validated data into a canonical format suitable for storage,
 * normalizing field names and structure.
 */
public class TransformDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wt_transform_data";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> validatedData = (Map<String, Object>) task.getInputData().get("validatedData");
        if (validatedData == null) {
            validatedData = Map.of();
        }

        String targetFormat = (String) task.getInputData().get("targetFormat");
        if (targetFormat == null || targetFormat.isBlank()) {
            targetFormat = "canonical";
        }

        System.out.println("  [wt_transform_data] Transforming data to " + targetFormat + " format");

        String orderId = (String) validatedData.getOrDefault("orderId", "unknown");
        String customer = (String) validatedData.getOrDefault("customer", "unknown");

        Object amountObj = validatedData.getOrDefault("amount", 0.0);
        double amount;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        } else {
            amount = 0.0;
        }

        String currency = (String) validatedData.getOrDefault("currency", "USD");

        Object itemsObj = validatedData.getOrDefault("items", 0);
        int items;
        if (itemsObj instanceof Number) {
            items = ((Number) itemsObj).intValue();
        } else {
            items = 0;
        }

        Map<String, Object> transformedData = Map.of(
                "id", orderId,
                "account", customer,
                "total", Map.of("value", amount, "currency", currency),
                "lineItemCount", items,
                "normalizedAt", "2026-01-15T10:00:00Z"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformedData", transformedData);
        result.getOutputData().put("destination", "orders_db");
        return result;
    }
}
