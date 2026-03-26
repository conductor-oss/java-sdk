package reportgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formats aggregated data into a report document.
 * Input: aggregated (map), reportType, dateRange
 * Output: report (map), format, reportUrl, pageCount
 */
public class FormatReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rg_format_report";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> agg = (Map<String, Object>) task.getInputData().getOrDefault("aggregated", Map.of());
        Map<String, Object> dateRange = (Map<String, Object>) task.getInputData().getOrDefault("dateRange", Map.of());

        String start = (String) dateRange.getOrDefault("start", "?");
        String end = (String) dateRange.getOrDefault("end", "?");

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("title", "Sales Report: " + start + " to " + end);
        report.put("generatedAt", "2024-03-15T15:00:00Z");
        report.put("summary", agg);
        report.put("sections", List.of("Executive Summary", "Regional Breakdown", "Product Analysis", "Trends"));

        List<String> sections = (List<String>) report.get("sections");
        String reportUrl = "s3://reports/sales-report-2024-03.pdf";

        System.out.println("  [format] Formatted PDF report with " + sections.size() + " sections");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", report);
        result.getOutputData().put("format", "PDF");
        result.getOutputData().put("reportUrl", reportUrl);
        result.getOutputData().put("pageCount", 8);
        return result;
    }
}
