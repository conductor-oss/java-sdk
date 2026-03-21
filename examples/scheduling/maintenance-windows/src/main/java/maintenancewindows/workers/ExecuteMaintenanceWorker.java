package maintenancewindows.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ExecuteMaintenanceWorker implements Worker {
    @Override public String getTaskDefName() { return "mnw_execute_maintenance"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [execute] Running " + task.getInputData().get("maintenanceType") + " on " + task.getInputData().get("system"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("executed", true);
        r.getOutputData().put("maintenanceType", task.getInputData().get("maintenanceType"));
        r.getOutputData().put("durationMs", 45000);
        r.getOutputData().put("tasksCompleted", List.of("db-vacuum", "index-rebuild", "cache-clear"));
        return r;
    }
}
