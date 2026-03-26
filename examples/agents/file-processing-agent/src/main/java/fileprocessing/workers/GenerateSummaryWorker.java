package fileprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Generates a human-readable summary from the analysis results and key findings.
 */
public class GenerateSummaryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fp_generate_summary";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String fileName = (String) task.getInputData().get("fileName");
        if (fileName == null || fileName.isBlank()) {
            fileName = "unknown_file";
        }

        String fileType = (String) task.getInputData().get("fileType");
        if (fileType == null || fileType.isBlank()) {
            fileType = "unknown";
        }

        Map<String, Object> analysis = (Map<String, Object>) task.getInputData().get("analysis");
        List<String> keyFindings = (List<String>) task.getInputData().get("keyFindings");

        System.out.println("  [fp_generate_summary] Generating summary for: " + fileName);

        String documentType = "unknown";
        String company = "Unknown";
        String timePeriod = "Unknown";
        String sentiment = "neutral";

        if (analysis != null) {
            documentType = (String) analysis.getOrDefault("documentType", "unknown");
            company = (String) analysis.getOrDefault("company", "Unknown");
            timePeriod = (String) analysis.getOrDefault("timePeriod", "Unknown");
            sentiment = (String) analysis.getOrDefault("sentiment", "neutral");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Document Summary: ").append(fileName).append("\n");
        sb.append("Type: ").append(documentType).append("\n");
        sb.append("Company: ").append(company).append("\n");
        sb.append("Period: ").append(timePeriod).append("\n");
        sb.append("Overall Sentiment: ").append(sentiment).append("\n\n");
        sb.append("Key Findings:\n");

        if (keyFindings != null) {
            for (int i = 0; i < keyFindings.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(keyFindings.get(i)).append("\n");
            }
        }

        String summary = sb.toString();

        // Count words in the summary
        String[] words = summary.trim().split("\\s+");
        int summaryWordCount = words.length;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("confidence", 0.93);
        result.getOutputData().put("summaryWordCount", summaryWordCount);
        result.getOutputData().put("documentType", documentType);
        return result;
    }
}
