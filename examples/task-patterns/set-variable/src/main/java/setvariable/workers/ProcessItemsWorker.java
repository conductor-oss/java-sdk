package setvariable.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Processes a list of items: computes total amount, count, and category.
 *
 * Category rules:
 * - high-value: total >= 1000
 * - standard:   total >= 100
 * - micro:      total < 100
 */
public class ProcessItemsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sv_process_items";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> items =
                (List<Map<String, Object>>) task.getInputData().get("items");

        if (items == null || items.isEmpty()) {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "No items provided");
            return result;
        }

        double totalAmount = 0;
        for (Map<String, Object> item : items) {
            Object amountObj = item.get("amount");
            if (amountObj instanceof Number) {
                totalAmount += ((Number) amountObj).doubleValue();
            }
        }

        int itemCount = items.size();

        String category;
        if (totalAmount >= 1000) {
            category = "high-value";
        } else if (totalAmount >= 100) {
            category = "standard";
        } else {
            category = "micro";
        }

        System.out.println("  [sv_process_items] Processed " + itemCount + " items, total=$"
                + totalAmount + ", category=" + category);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("totalAmount", totalAmount);
        result.getOutputData().put("itemCount", itemCount);
        result.getOutputData().put("category", category);
        return result;
    }
}
