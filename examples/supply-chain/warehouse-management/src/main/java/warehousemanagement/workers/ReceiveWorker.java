package warehousemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class ReceiveWorker implements Worker {
    @Override public String getTaskDefName() { return "wm_receive"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        List<Map<String, Object>> items = (List<Map<String, Object>>) task.getInputData().get("items");
        if (items == null) items = List.of();
        System.out.println("  [receive] Order " + orderId + ": " + items.size() + " items received at dock");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("receivedItems", items);
        return r;
    }
}
