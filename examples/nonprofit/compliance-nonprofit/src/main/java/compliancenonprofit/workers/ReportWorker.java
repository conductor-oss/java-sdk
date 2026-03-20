package compliancenonprofit.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "cnp_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Generating compliance report for " + task.getInputData().get("organizationName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("report", Map.of("title", "Annual Compliance Report", "status", "FULLY_COMPLIANT", "findings", 0)); return r;
    }
}
