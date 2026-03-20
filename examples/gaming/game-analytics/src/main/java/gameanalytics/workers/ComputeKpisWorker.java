package gameanalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ComputeKpisWorker implements Worker {
    @Override public String getTaskDefName() { return "gan_compute_kpis"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [kpis] Computing KPIs");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("kpis", Map.of("dau", 3200, "arpdau", 0.85, "retention7d", "45%", "avgSession", "42 min", "conversionRate", "3.8%"));
        return r;
    }
}
