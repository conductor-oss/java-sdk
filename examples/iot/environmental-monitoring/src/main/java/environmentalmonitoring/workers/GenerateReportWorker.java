package environmentalmonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateReportWorker implements Worker {
    @Override public String getTaskDefName() { return "env_generate_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Processing " + task.getInputData().getOrDefault("reportId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reportId", "ENV-RPT-539-001");
        r.getOutputData().put("reportUrl", "https://env.example.com/reports/539-001");
        r.getOutputData().put("generatedAt", "2026-03-08T12:05:00Z");
        r.getOutputData().put("complianceStatus", "under_review");
        return r;
    }
}
