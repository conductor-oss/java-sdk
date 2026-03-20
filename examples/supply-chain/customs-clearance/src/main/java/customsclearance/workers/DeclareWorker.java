package customsclearance.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DeclareWorker implements Worker {
    @Override public String getTaskDefName() { return "cst_declare"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [declare] Shipment " + task.getInputData().get("shipmentId") + " from " + task.getInputData().get("origin"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("declarationId", "DEC-666-001"); return r;
    }
}
