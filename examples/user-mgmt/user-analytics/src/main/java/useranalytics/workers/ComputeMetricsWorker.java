package useranalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class ComputeMetricsWorker implements Worker {
    @Override public String getTaskDefName() { return "uan_compute_metrics"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [metrics] Key metrics computed: DAU=8450, MAU=12340, retention=68.5%");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("metrics", Map.of("dau", 8450, "mau", 12340, "retention", "68.5%", "avgSessionMinutes", 12.3, "churnRate", "3.2%"));
        return r;
    }
}
