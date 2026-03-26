package inapppurchase.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DeliverWorker implements Worker {
    @Override public String getTaskDefName() { return "iap_deliver"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [deliver] Delivering item " + task.getInputData().get("itemId") + " to " + task.getInputData().get("playerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("delivered", true); r.addOutputData("inventoryUpdated", true);
        return r;
    }
}
