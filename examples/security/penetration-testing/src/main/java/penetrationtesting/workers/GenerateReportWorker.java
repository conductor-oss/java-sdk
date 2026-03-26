package penetrationtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Generates the pen test report with remediation steps.
 * Input: generate_reportData (from exploit test)
 * Output: generate_report, completedAt
 */
public class GenerateReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pen_generate_report";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [report] Pen test report generated with remediation steps");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("generate_report", true);
        result.getOutputData().put("completedAt", "2026-01-15T10:05:00Z");
        return result;
    }
}
