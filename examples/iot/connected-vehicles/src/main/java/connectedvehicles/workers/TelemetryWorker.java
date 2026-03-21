package connectedvehicles.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class TelemetryWorker implements Worker {
    @Override public String getTaskDefName() { return "veh_telemetry"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [telemetry] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("done", true);
        return r;
    }
}
