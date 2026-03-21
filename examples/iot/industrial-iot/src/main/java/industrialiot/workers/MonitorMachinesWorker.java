package industrialiot.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class MonitorMachinesWorker implements Worker {
    @Override public String getTaskDefName() { return "iit_monitor_machines"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [monitor] Processing " + task.getInputData().getOrDefault("telemetry", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("telemetry", Map.of());
        r.getOutputData().put("temperature", 185);
        r.getOutputData().put("vibration", 4.2);
        r.getOutputData().put("pressure", 120);
        r.getOutputData().put("rpm", 3400);
        r.getOutputData().put("oilLevel", 72);
        r.getOutputData().put("runHours", 12500);
        return r;
    }
}
