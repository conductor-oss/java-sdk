package agentsupervisor.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that reviews results from all agents and produces a final report.
 * Takes plan, code result, test result, doc result, and system prompt.
 * Returns a comprehensive report with statuses, action items, and metrics.
 */
public class ReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sup_review";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> plan = (Map<String, Object>) task.getInputData().get("plan");
        Map<String, Object> codeResult = (Map<String, Object>) task.getInputData().get("codeResult");
        Map<String, Object> testResult = (Map<String, Object>) task.getInputData().get("testResult");
        Map<String, Object> docResult = (Map<String, Object>) task.getInputData().get("docResult");

        String feature = plan != null ? (String) plan.get("feature") : "unknown";
        String codeStatus = codeResult != null ? (String) codeResult.get("status") : "unknown";
        String testStatus = testResult != null ? (String) testResult.get("status") : "unknown";
        String docStatus = docResult != null ? (String) docResult.get("status") : "unknown";

        // Determine overall status based on agent results
        String overallStatus;
        if ("implemented".equals(codeStatus) && "complete".equals(docStatus)
                && "needs_fix".equals(testStatus)) {
            overallStatus = "NEEDS_REVISION";
        } else if ("implemented".equals(codeStatus)
                && "complete".equals(docStatus)
                && "passed".equals(testStatus)) {
            overallStatus = "APPROVED";
        } else {
            overallStatus = "NEEDS_REVISION";
        }

        List<String> actionItems = List.of(
                "Fix failing test: testTokenExpirationEdgeCase",
                "Increase test coverage from 82% to at least 90%",
                "Add error handling documentation section"
        );

        int linesOfCode = codeResult != null && codeResult.get("linesOfCode") instanceof Number n
                ? n.intValue() : 0;
        int totalTests = testResult != null && testResult.get("totalTests") instanceof Number n
                ? n.intValue() : 0;
        int wordCount = docResult != null && docResult.get("wordCount") instanceof Number n
                ? n.intValue() : 0;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("linesOfCode", linesOfCode);
        metrics.put("totalTests", totalTests);
        metrics.put("documentationWords", wordCount);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("feature", feature);
        report.put("overallStatus", overallStatus);
        report.put("codeStatus", codeStatus);
        report.put("testStatus", testStatus);
        report.put("docStatus", docStatus);
        report.put("actionItems", actionItems);
        report.put("metrics", metrics);
        report.put("agentsUsed", List.of("coder", "tester", "documenter"));

        System.out.println("  [review] Review complete for '" + feature + "': " + overallStatus);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", report);
        return result;
    }
}
