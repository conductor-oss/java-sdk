package analyticsreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class GenerateReportWorker implements Worker {
    @Override public String getTaskDefName() { return "anr_generate_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Processing " + task.getInputData().getOrDefault("reportUrl", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reportUrl", "https://reports.example.com/analytics/526");
        r.getOutputData().put("format", "interactive_dashboard");
        r.getOutputData().put("generatedAt", "2026-03-08T08:00:00Z");
        r.getOutputData().put("scheduledDelivery", List.of("email:team@example.com", "slack:#analytics"));
        return r;
    }
}
