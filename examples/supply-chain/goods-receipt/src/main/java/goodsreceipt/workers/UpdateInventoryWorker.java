package goodsreceipt.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class UpdateInventoryWorker implements Worker {
    @Override public String getTaskDefName() { return "grc_update_inventory"; }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> storedItems = (List<Map<String, Object>>) task.getInputData().get("storedItems");
        int count = storedItems != null ? storedItems.size() : 0;
        System.out.println("  [update] Inventory updated for " + count + " items");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("updated", true);
        result.getOutputData().put("itemsUpdated", count);
        return result;
    }
}
