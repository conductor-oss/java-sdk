package webbrowsing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Selects the most relevant pages from search results based on relevance score.
 * Sorts by relevance descending and returns the top N pages.
 */
public class SelectPagesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wb_select_pages";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> searchResults =
                (List<Map<String, Object>>) task.getInputData().get("searchResults");
        String question = (String) task.getInputData().get("question");
        Object maxPagesObj = task.getInputData().get("maxPages");

        int maxPages = 3;
        if (maxPagesObj instanceof Number) {
            maxPages = ((Number) maxPagesObj).intValue();
        }

        if (searchResults == null || searchResults.isEmpty()) {
            searchResults = List.of();
        }

        if (question == null || question.isBlank()) {
            question = "general query";
        }

        System.out.println("  [wb_select_pages] Selecting top " + maxPages
                + " pages from " + searchResults.size() + " results for: " + question);

        // Sort by relevance score descending
        List<Map<String, Object>> sorted = new ArrayList<>(searchResults);
        sorted.sort(Comparator.<Map<String, Object>, Double>comparing(
                m -> ((Number) m.getOrDefault("relevance", 0)).doubleValue()
        ).reversed());

        int count = Math.min(maxPages, sorted.size());
        List<Map<String, Object>> selectedPages = sorted.subList(0, count);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("selectedPages", selectedPages);
        result.getOutputData().put("pageCount", count);
        return result;
    }
}
