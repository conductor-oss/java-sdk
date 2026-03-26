package scheduledreports.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Distributes a report to recipients.
 * Input: reportUrl, recipients, reportType
 * Output: delivered, recipientCount, method, sentAt
 */
public class DistributeReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sch_distribute_report";
    }

    @Override
    public TaskResult execute(Task task) {
        String reportType = (String) task.getInputData().get("reportType");
        if (reportType == null) reportType = "unknown";

        System.out.println("  [distribute] Sending " + reportType + " report to recipients...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delivered", true);
        result.getOutputData().put("recipientCount", 5);
        result.getOutputData().put("method", "email");
        result.getOutputData().put("sentAt", "2026-03-08T06:00:00Z");
        return result;
    }
}
