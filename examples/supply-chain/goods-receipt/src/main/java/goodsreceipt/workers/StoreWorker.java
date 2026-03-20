package goodsreceipt.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class StoreWorker implements Worker {
    @Override public String getTaskDefName() { return "grc_store"; }

    @Override
    public TaskResult execute(Task task) {
        Object inspectedItems = task.getInputData().get("inspectedItems");
        System.out.println("  [store] Items stored in Warehouse A, Aisle 12");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("storedItems", inspectedItems);
        result.getOutputData().put("location", "WH-A-12");
        return result;
    }
}
