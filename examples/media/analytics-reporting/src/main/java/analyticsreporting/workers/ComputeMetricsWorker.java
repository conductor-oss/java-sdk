package analyticsreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class ComputeMetricsWorker implements Worker {
    @Override public String getTaskDefName() { return "anr_compute_metrics"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [metrics] Processing " + task.getInputData().getOrDefault("metrics", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("metrics", Map.of());
        r.getOutputData().put("dau", 6000);
        r.getOutputData().put("wau", 28000);
        r.getOutputData().put("mau", 42000);
        r.getOutputData().put("bounceRate", 38.5);
        r.getOutputData().put("conversionRate", 4.2);
        r.getOutputData().put("revenuePerUser", 12.50);
        return r;
    }
}
