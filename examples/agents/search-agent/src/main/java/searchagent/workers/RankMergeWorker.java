package searchagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Merges Google and Wikipedia results, sorts by relevance descending,
 * and returns the ranked results along with top source titles.
 */
public class RankMergeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sa_rank_merge";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "general query";
        }

        List<Map<String, Object>> googleResults =
                (List<Map<String, Object>>) task.getInputData().get("googleResults");
        List<Map<String, Object>> wikiResults =
                (List<Map<String, Object>>) task.getInputData().get("wikiResults");

        if (googleResults == null) {
            googleResults = List.of();
        }
        if (wikiResults == null) {
            wikiResults = List.of();
        }

        System.out.println("  [sa_rank_merge] Merging " + googleResults.size()
                + " Google + " + wikiResults.size() + " Wiki results for: " + question);

        // Merge all results into a single list
        List<Map<String, Object>> merged = new ArrayList<>();
        merged.addAll(googleResults);
        merged.addAll(wikiResults);

        // Sort by relevance descending
        merged.sort(Comparator.comparingDouble(
                (Map<String, Object> item) -> ((Number) item.getOrDefault("relevance", 0)).doubleValue()
        ).reversed());

        // Extract top 3 source titles
        List<String> topSources = new ArrayList<>();
        for (int i = 0; i < Math.min(3, merged.size()); i++) {
            Object title = merged.get(i).get("title");
            if (title != null) {
                topSources.add(title.toString());
            }
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rankedResults", merged);
        result.getOutputData().put("topSources", topSources);
        result.getOutputData().put("totalResults", merged.size());
        return result;
    }
}
