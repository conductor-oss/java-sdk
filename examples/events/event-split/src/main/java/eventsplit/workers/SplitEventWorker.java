package eventsplit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Splits a composite event into three sub-events.
 * Input: event (map with order, customer, shipping)
 * Output: subEventA (order_details), subEventB (customer_info), subEventC (shipping_info), subEventCount (3)
 */
public class SplitEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sp_split_event";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> event = (Map<String, Object>) task.getInputData().get("event");
        if (event == null) {
            event = Map.of();
        }

        Object order = event.getOrDefault("order", Map.of());
        Object customer = event.getOrDefault("customer", Map.of());
        Object shipping = event.getOrDefault("shipping", Map.of());

        Map<String, Object> subEventA = Map.of("type", "order_details", "data", order);
        Map<String, Object> subEventB = Map.of("type", "customer_info", "data", customer);
        Map<String, Object> subEventC = Map.of("type", "shipping_info", "data", shipping);

        System.out.println("  [sp_split_event] Split into 3 sub-events: order_details, customer_info, shipping_info");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("subEventA", subEventA);
        result.getOutputData().put("subEventB", subEventB);
        result.getOutputData().put("subEventC", subEventC);
        result.getOutputData().put("subEventCount", 3);
        return result;
    }
}
