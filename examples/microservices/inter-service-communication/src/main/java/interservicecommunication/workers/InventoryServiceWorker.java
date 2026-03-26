package interservicecommunication.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class InventoryServiceWorker implements Worker {
    @Override public String getTaskDefName() { return "isc_inventory_service"; }
    @Override public TaskResult execute(Task task) {
        String orderRef = (String) task.getInputData().getOrDefault("orderRef", "unknown");
        System.out.println("  [inventory] Reserving items for " + orderRef);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reserved", true);
        r.getOutputData().put("warehouse", "WH-EAST-1");
        return r;
    }
}
