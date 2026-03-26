package multiagentcodereview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Compile review agent — aggregates findings from security, performance,
 * and style review agents, counts total and high-severity issues,
 * and produces an overall review summary.
 */
public class CompileReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_compile_review";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> securityFindings =
                (List<Map<String, Object>>) task.getInputData().get("securityFindings");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> performanceFindings =
                (List<Map<String, Object>>) task.getInputData().get("performanceFindings");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> styleFindings =
                (List<Map<String, Object>>) task.getInputData().get("styleFindings");

        if (securityFindings == null) securityFindings = List.of();
        if (performanceFindings == null) performanceFindings = List.of();
        if (styleFindings == null) styleFindings = List.of();

        int totalIssues = securityFindings.size() + performanceFindings.size() + styleFindings.size();

        int highCount = 0;
        highCount += countBySeverity(securityFindings, "HIGH");
        highCount += countBySeverity(performanceFindings, "HIGH");
        highCount += countBySeverity(styleFindings, "HIGH");

        String overallSeverity;
        if (highCount >= 2) {
            overallSeverity = "CRITICAL";
        } else if (highCount == 1) {
            overallSeverity = "HIGH";
        } else {
            overallSeverity = "LOW";
        }

        String summary = "Code review complete: " + totalIssues + " issues found ("
                + highCount + " high severity). "
                + "Security: " + securityFindings.size() + ", "
                + "Performance: " + performanceFindings.size() + ", "
                + "Style: " + styleFindings.size() + ". "
                + "Overall severity: " + overallSeverity + ".";

        System.out.println("  [cr_compile_review] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("totalIssues", totalIssues);
        result.getOutputData().put("highCount", highCount);
        result.getOutputData().put("overallSeverity", overallSeverity);
        result.getOutputData().put("summary", summary);
        return result;
    }

    private int countBySeverity(List<Map<String, Object>> findings, String severity) {
        int count = 0;
        for (Map<String, Object> finding : findings) {
            if (severity.equals(finding.get("severity"))) {
                count++;
            }
        }
        return count;
    }
}
