package logprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts recurring patterns from parsed log entries.
 * Input: entries
 * Output: patterns, patternCount, topPattern
 */
public class ExtractPatternsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lp_extract_patterns";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> entries = (List<Map<String, Object>>) task.getInputData().get("entries");
        if (entries == null) {
            entries = List.of();
        }

        Map<String, Integer> msgCounts = new LinkedHashMap<>();
        for (Map<String, Object> e : entries) {
            String msg = (String) e.getOrDefault("message", "");
            String key = msg.replaceAll("[A-Z]-\\d+", "X").replaceAll("/\\w+", "/*");
            msgCounts.merge(key, 1, Integer::sum);
        }

        List<Map<String, Object>> patterns = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : msgCounts.entrySet()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("pattern", entry.getKey());
            p.put("count", entry.getValue());
            patterns.add(p);
        }
        patterns.sort(Comparator.comparingInt((Map<String, Object> m) -> (int) m.get("count")).reversed());

        String topPattern = patterns.isEmpty() ? "none" : (String) patterns.get(0).get("pattern");
        int topCount = patterns.isEmpty() ? 0 : (int) patterns.get(0).get("count");

        System.out.println("  [patterns] Found " + patterns.size() + " distinct patterns, top: \"" + topPattern + "\" (" + topCount + "x)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("patterns", patterns);
        result.getOutputData().put("patternCount", patterns.size());
        result.getOutputData().put("topPattern", topPattern);
        return result;
    }
}
