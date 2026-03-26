package maintenancewindows.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeferMaintenanceWorker implements Worker {
    @Override public String getTaskDefName() { return "mnw_defer_maintenance"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [defer] Deferring " + task.getInputData().get("maintenanceType") + " to " + task.getInputData().get("nextWindow"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("deferred", true);
        r.getOutputData().put("scheduledFor", task.getInputData().get("nextWindow"));
        r.getOutputData().put("reason", "Outside maintenance window");
        return r;
    }
}
