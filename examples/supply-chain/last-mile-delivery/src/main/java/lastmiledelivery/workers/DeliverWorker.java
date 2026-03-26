package lastmiledelivery.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DeliverWorker implements Worker {
    @Override public String getTaskDefName() { return "lmd_deliver"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [deliver] " + task.getInputData().get("driverId") + " delivered order " + task.getInputData().get("orderId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("status", "delivered"); r.getOutputData().put("photoProof", true); return r;
    }
}
