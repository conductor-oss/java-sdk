package pdfprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a summary based on sections and analysis.
 * Input: sections (list of maps), analysis (map with avgWordsPerSection)
 * Output: summary (string)
 */
public class GenerateSummaryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pd_generate_summary";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> sections = (List<Map<String, Object>>) task.getInputData().get("sections");
        if (sections == null) {
            sections = List.of();
        }

        Map<String, Object> analysis = (Map<String, Object>) task.getInputData().get("analysis");
        if (analysis == null) {
            analysis = Map.of();
        }

        String titles = sections.stream()
                .map(s -> ((String) s.get("title")).toLowerCase())
                .collect(Collectors.joining(", "));

        Object avgObj = analysis.get("avgWordsPerSection");
        int avg = 0;
        if (avgObj instanceof Number) {
            avg = ((Number) avgObj).intValue();
        }

        String summary = "Document contains " + sections.size() + " chapters covering " + titles
                + ". Average " + avg + " words per section.";

        System.out.println("  [summary] Generated summary (" + summary.length() + " chars)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
