package connectedvehicles.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DiagnosticsWorker implements Worker {
    @Override public String getTaskDefName() { return "veh_diagnostics"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [diagnostics] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("overallHealth", "health");
        r.getOutputData().put("batteryOk", "battery > 12.0");
        return r;
    }
}
