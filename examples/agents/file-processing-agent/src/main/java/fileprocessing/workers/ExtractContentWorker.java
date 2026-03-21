package fileprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Extracts content from a file. Returns structured content
 * (title, sections, pages) and metadata (word count, page count, etc.).
 */
public class ExtractContentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fp_extract_content";
    }

    @Override
    public TaskResult execute(Task task) {
        String fileName = (String) task.getInputData().get("fileName");
        if (fileName == null || fileName.isBlank()) {
            fileName = "unknown_file";
        }

        String extractionMethod = (String) task.getInputData().get("extractionMethod");
        if (extractionMethod == null) {
            extractionMethod = "raw_text";
        }

        System.out.println("  [fp_extract_content] Extracting content from: " + fileName
                + " using " + extractionMethod);

        Map<String, Object> content = Map.of(
                "title", "Q4 2025 Financial Report — Acme Corporation",
                "sections", List.of(
                        Map.of("heading", "Executive Summary",
                                "text", "Acme Corporation reported record revenue of $2.4B in Q4 2025, representing a 28% year-over-year increase driven by strong enterprise adoption."),
                        Map.of("heading", "Revenue Breakdown",
                                "text", "Enterprise segment contributed $1.6B (67%), while consumer segment added $800M (33%). International markets grew 45% to represent 38% of total revenue."),
                        Map.of("heading", "Key Metrics",
                                "text", "Gross margin improved to 72% from 68% in Q4 2024. Customer retention rate reached 94%. Net new enterprise customers: capacity-planning."),
                        Map.of("heading", "Outlook",
                                "text", "Management raised FY2026 guidance to $11B-$11.5B, reflecting confidence in the AI product pipeline and expanding enterprise partnerships.")
                ),
                "pages", 24
        );

        Map<String, Object> metadata = Map.of(
                "wordCount", 156,
                "pageCount", 24,
                "sectionCount", 4,
                "hasImages", true,
                "hasTables", true,
                "language", "en"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("content", content);
        result.getOutputData().put("metadata", metadata);
        return result;
    }
}
