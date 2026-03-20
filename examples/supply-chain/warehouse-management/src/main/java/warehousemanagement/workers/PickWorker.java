package warehousemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class PickWorker implements Worker {
    @Override public String getTaskDefName() { return "wm_pick"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> locs = (List<Map<String, Object>>) task.getInputData().get("locations");
        if (locs == null) locs = List.of();
        System.out.println("  [pick] Picked " + locs.size() + " items for order " + task.getInputData().get("orderId"));
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("pickedItems", locs);
        r.getOutputData().put("pickTime", "12 min");
        return r;
    }
}
