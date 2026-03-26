package tooluseconditional.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Handles search-category queries by performing a web search tool.
 * Returns an answer string, a list of search results, totalFound count,
 * and the toolUsed identifier.
 */
public class WebSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tc_web_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String searchQuery = (String) task.getInputData().get("searchQuery");
        if (searchQuery == null || searchQuery.isBlank()) {
            searchQuery = "general search";
        }

        String userQuery = (String) task.getInputData().get("userQuery");
        if (userQuery == null || userQuery.isBlank()) {
            userQuery = searchQuery;
        }

        System.out.println("  [tc_web_search] Searching for: " + searchQuery);

        List<Map<String, Object>> results = List.of(
                Map.of(
                        "title", "Comprehensive Guide to " + searchQuery,
                        "url", "https://example.com/guide/" + searchQuery.replaceAll("\\s+", "-").toLowerCase(),
                        "snippet", "An in-depth exploration of " + searchQuery + " covering key concepts, best practices, and recent developments."
                ),
                Map.of(
                        "title", searchQuery + " — Latest Research and Insights",
                        "url", "https://example.com/research/" + searchQuery.replaceAll("\\s+", "-").toLowerCase(),
                        "snippet", "Recent findings and expert analysis on " + searchQuery + " with practical applications."
                )
        );

        String answer = "Found 1250 results for '" + searchQuery + "'. "
                + "Top result: " + results.get(0).get("title");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("results", results);
        result.getOutputData().put("totalFound", 1250);
        result.getOutputData().put("toolUsed", "web_search");
        return result;
    }
}
