package eventchoreography.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Reserves inventory items for an order.
 * Input: orderId, items
 * Output: reserved (true), itemCount (2)
 */
public class InventoryServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ch_inventory_service";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        Object items = task.getInputData().get("items");
        if (items == null) {
            items = List.of();
        }

        System.out.println("  [ch_inventory_service] Reserved items for order " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reserved", true);
        result.getOutputData().put("itemCount", 2);
        return result;
    }
}
