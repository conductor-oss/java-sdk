package lastmiledelivery.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AssignDriverWorker implements Worker {
    @Override public String getTaskDefName() { return "lmd_assign_driver"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [assign] Order " + task.getInputData().get("orderId") + " assigned to driver DRV-42");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("driverId", "DRV-42"); r.getOutputData().put("driverName", "Mike Johnson"); return r;
    }
}
