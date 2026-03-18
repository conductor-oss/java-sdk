package pdfprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzes content of parsed sections — counts words and finds keywords.
 * Input: sections (list of maps with title/content)
 * Output: wordCount, keywords, analysis (avgWordsPerSection)
 */
public class AnalyzeContentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pd_analyze_content";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, String>> sections = (List<Map<String, String>>) task.getInputData().get("sections");
        if (sections == null) {
            sections = List.of();
        }

        StringBuilder allTextBuilder = new StringBuilder();
        for (Map<String, String> section : sections) {
            if (allTextBuilder.length() > 0) {
                allTextBuilder.append(" ");
            }
            allTextBuilder.append(section.get("content"));
        }
        String allText = allTextBuilder.toString();

        String[] wordsArr = allText.split("\\s+");
        List<String> words = new ArrayList<>();
        for (String w : wordsArr) {
            if (!w.isEmpty()) {
                words.add(w);
            }
        }

        List<String> keywordCandidates = Arrays.asList("data", "processing", "architecture", "pipelines", "analytics");
        List<String> found = new ArrayList<>();
        String lowerText = allText.toLowerCase();
        for (String k : keywordCandidates) {
            if (lowerText.contains(k)) {
                found.add(k);
            }
        }

        int avgWordsPerSection = sections.isEmpty() ? 0 : Math.round((float) words.size() / sections.size());

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("avgWordsPerSection", avgWordsPerSection);

        System.out.println("  [analyze] " + words.size() + " words, " + found.size() + " keywords detected: "
                + String.join(", ", found));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("wordCount", words.size());
        result.getOutputData().put("keywords", found);
        result.getOutputData().put("analysis", analysis);
        return result;
    }
}
