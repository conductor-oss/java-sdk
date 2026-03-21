package backgroundcheck.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "bgc_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] All checks passed — report generated");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("overallResult", "pass");
        r.getOutputData().put("reportId", "BGC-RPT-710");
        return r;
    }
}
