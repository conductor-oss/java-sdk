package securitytraining.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Generates training compliance report.
 * Input: report_complianceData (from evaluate step)
 * Output: report_compliance, completedAt
 */
public class StReportComplianceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_report_compliance";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [report] Training compliance report generated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report_compliance", true);
        result.getOutputData().put("completedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
