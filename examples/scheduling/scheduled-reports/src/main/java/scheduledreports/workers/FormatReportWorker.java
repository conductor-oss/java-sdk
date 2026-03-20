package scheduledreports.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Formats a report from queried data.
 * Input: reportType, data, rowCount
 * Output: reportUrl, format, sizeKb, pages
 */
public class FormatReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sch_format_report";
    }

    @Override
    public TaskResult execute(Task task) {
        String reportType = (String) task.getInputData().get("reportType");
        Object rowCount = task.getInputData().get("rowCount");
        if (reportType == null) reportType = "unknown";

        System.out.println("  [format] Formatting " + reportType + " report with " + rowCount + " rows...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportUrl", "https://reports.example.com/weekly-sales-20260308.pdf");
        result.getOutputData().put("format", "PDF");
        result.getOutputData().put("sizeKb", 342);
        result.getOutputData().put("pages", 8);
        return result;
    }
}
