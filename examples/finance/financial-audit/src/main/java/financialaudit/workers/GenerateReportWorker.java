package financialaudit.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class GenerateReportWorker implements Worker {
    @Override public String getTaskDefName() { return "fau_generate_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Generating audit report for " + task.getInputData().get("entityName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reportId", "AUD-RPT-508-2026"); r.getOutputData().put("opinion", "unqualified");
        return r;
    }
}
