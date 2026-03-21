package customsclearance.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReleaseWorker implements Worker {
    @Override public String getTaskDefName() { return "cst_release"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [release] Shipment " + task.getInputData().get("shipmentId") + " released from customs");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("released", true); return r;
    }
}
