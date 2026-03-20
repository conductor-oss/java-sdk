package freightmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DeliverWorker implements Worker {
    @Override public String getTaskDefName() { return "frm_deliver"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [deliver] " + task.getInputData().get("bookingId") + ": delivered and signed");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("delivered", true); r.getOutputData().put("signedBy", "Warehouse Mgr"); return r;
    }
}
