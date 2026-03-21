package certificatemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class InventoryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_inventory";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [inventory] Scanned all-environments: 87 certificates found");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("inventoryId", "INVENTORY-1354");
        result.addOutputData("success", true);
        return result;
    }
}
