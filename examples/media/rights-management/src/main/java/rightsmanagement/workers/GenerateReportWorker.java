package rightsmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateReportWorker implements Worker {
    @Override public String getTaskDefName() { return "rts_generate_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Processing " + task.getInputData().getOrDefault("reportId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reportId", "RTS-RPT-529-001");
        r.getOutputData().put("generatedAt", "2026-03-08T10:30:00Z");
        r.getOutputData().put("format", "pdf");
        r.getOutputData().put("reportUrl", "https://reports.example.com/rights/529-001.pdf");
        return r;
    }
}
