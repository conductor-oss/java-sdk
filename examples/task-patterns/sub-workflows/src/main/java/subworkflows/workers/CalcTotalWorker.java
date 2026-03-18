package subworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Calculates the order total from a list of items.
 * Each item has name, price, and qty. Total = sum of (price * qty) for all items.
 */
public class CalcTotalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sub_calc_total";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items =
                (List<Map<String, Object>>) task.getInputData().get("items");

        if (items == null || items.isEmpty()) {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("total", 0.0);
            result.getOutputData().put("itemCount", 0);
            return result;
        }

        double total = 0.0;
        for (Map<String, Object> item : items) {
            double price = toDouble(item.get("price"));
            int qty = toInt(item.get("qty"));
            total += price * qty;
        }

        System.out.println("  [sub_calc_total] Calculated total: " + total + " from " + items.size() + " items");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("total", total);
        result.getOutputData().put("itemCount", items.size());
        return result;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}
