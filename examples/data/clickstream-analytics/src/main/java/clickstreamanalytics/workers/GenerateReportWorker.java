package clickstreamanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a clickstream analytics report.
 * Input: sessions, journeyAnalysis, analysisType
 * Output: report
 */
public class GenerateReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ck_generate_report";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> analysis = (Map<String, Object>) task.getInputData().getOrDefault("journeyAnalysis", Map.of());
        List<Map<String, Object>> sessions = (List<Map<String, Object>>) task.getInputData().getOrDefault("sessions", List.of());

        int conversions = analysis.containsKey("converted") ? ((Number) analysis.get("converted")).intValue() : 0;
        int bounces = analysis.containsKey("bounced") ? ((Number) analysis.get("bounced")).intValue() : 0;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", "2024-03-15 10:00-11:00");
        report.put("totalSessions", sessions.size());
        report.put("conversions", conversions);
        report.put("bounces", bounces);
        report.put("topPages", List.of("/home", "/products", "/cart"));

        System.out.println("  [report] Generated report: " + sessions.size() + " sessions, " + conversions + " conversions, " + bounces + " bounces");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", report);
        return result;
    }
}
