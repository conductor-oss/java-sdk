package uptimemonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CalculateSlaWorker implements Worker {
    @Override public String getTaskDefName() { return "um_calculate_sla"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [sla] Calculating SLA");
        r.getOutputData().put("currentSla", 99.95);
        r.getOutputData().put("slaMet", true);
        r.getOutputData().put("totalChecks", 8640);
        return r;
    }
}
