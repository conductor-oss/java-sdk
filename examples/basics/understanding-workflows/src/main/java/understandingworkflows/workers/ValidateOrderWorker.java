package understandingworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates an order by checking each item and marking it as validated and in stock.
 *
 * Input:  orderId (String), items (List of {name, price, qty})
 * Output: validatedItems (List of {name, price, qty, validated, inStock}), itemCount (int)
 */
public class ValidateOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "validate_order";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items =
                (List<Map<String, Object>>) task.getInputData().get("items");

        System.out.println("  [validate_order] Validating order " + orderId
                + " with " + (items != null ? items.size() : 0) + " items");

        List<Map<String, Object>> validatedItems = new ArrayList<>();
        if (items != null) {
            for (Map<String, Object> item : items) {
                Map<String, Object> validated = new HashMap<>(item);
                validated.put("validated", true);
                validated.put("inStock", true);
                validatedItems.add(validated);
            }
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validatedItems", validatedItems);
        result.getOutputData().put("itemCount", validatedItems.size());
        return result;
    }
}
