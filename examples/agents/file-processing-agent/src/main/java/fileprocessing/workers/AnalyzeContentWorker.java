package fileprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Analyzes extracted content — identifies document type, sentiment, topics,
 * named entities, and key findings.
 */
public class AnalyzeContentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fp_analyze_content";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object contentObj = task.getInputData().get("content");
        String fileType = (String) task.getInputData().get("fileType");
        if (fileType == null || fileType.isBlank()) {
            fileType = "unknown";
        }

        System.out.println("  [fp_analyze_content] Analyzing content of type: " + fileType);

        Map<String, Object> analysis = Map.of(
                "documentType", "financial_report",
                "timePeriod", "Q4 2025",
                "company", "Acme Corporation",
                "sentiment", "positive",
                "sentimentScore", 0.82,
                "topics", List.of("revenue", "growth", "enterprise", "AI", "margins"),
                "namedEntities", Map.of(
                        "organizations", List.of("Acme Corporation"),
                        "monetaryValues", List.of("$2.4B", "$1.6B", "$800M", "$11B", "$11.5B"),
                        "percentages", List.of("28%", "67%", "33%", "45%", "38%", "72%", "68%", "94%"),
                        "dates", List.of("Q4 2025", "Q4 2024", "FY2026")
                )
        );

        List<String> keyFindings = List.of(
                "Record quarterly revenue of $2.4B, up 28% year-over-year",
                "Enterprise segment dominates at 67% of total revenue ($1.6B)",
                "Customer retention rate of 94% indicates strong product-market fit",
                "Gross margin expansion from 68% to 72% shows improving operational efficiency",
                "FY2026 guidance raised to $11B-$11.5B, signaling management confidence"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("analysis", analysis);
        result.getOutputData().put("keyFindings", keyFindings);
        return result;
    }
}
