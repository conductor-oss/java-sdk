package shoppingcart.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adds items to a shopping cart and assigns line IDs.
 */
public class AddItemsWorker implements Worker {

    private static final AtomicLong COUNTER = new AtomicLong();

    @Override
    public String getTaskDefName() {
        return "cart_add_items";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        List<Map<String, Object>> items = (List<Map<String, Object>>) task.getInputData().get("items");
        if (items == null) items = List.of();

        String cartId = "cart-" + Long.toString(System.currentTimeMillis(), 36) + "-" + COUNTER.incrementAndGet();

        List<Map<String, Object>> cartItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> cartItem = new LinkedHashMap<>(items.get(i));
            cartItem.put("lineId", "line-" + (i + 1));
            cartItem.put("addedAt", Instant.now().toString());
            cartItems.add(cartItem);
        }

        System.out.println("  [add] Cart " + cartId + ": " + cartItems.size() + " items for user " + userId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("cartId", cartId);
        output.put("cartItems", cartItems);
        output.put("itemCount", cartItems.size());
        result.setOutputData(output);
        return result;
    }
}
