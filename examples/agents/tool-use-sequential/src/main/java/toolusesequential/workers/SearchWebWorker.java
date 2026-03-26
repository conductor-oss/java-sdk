package toolusesequential.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Performs a web search tool.
 * Takes a query and maxResults, returns a list of search results with url, title, and snippet.
 */
public class SearchWebWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ts_search_web";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        if (query == null || query.isBlank()) {
            query = "general search";
        }

        Object maxResultsObj = task.getInputData().get("maxResults");
        int maxResults = 5;
        if (maxResultsObj instanceof Number) {
            maxResults = ((Number) maxResultsObj).intValue();
        }

        System.out.println("  [ts_search_web] Searching for: " + query + " (maxResults=" + maxResults + ")");

        List<Map<String, String>> results = List.of(
                Map.of(
                        "url", "https://orkes.io/content/docs/conductor-overview",
                        "title", "Conductor Overview - Orkes Documentation",
                        "snippet", "Conductor is an open-source, distributed workflow orchestration engine designed to manage and execute complex workflows across microservices."
                ),
                Map.of(
                        "url", "https://netflix.github.io/conductor/",
                        "title", "Netflix Conductor - Workflow Orchestration Engine",
                        "snippet", "Netflix Conductor is a platform for orchestrating workflows that span across microservices. Originally developed at Netflix to manage media processing pipelines."
                ),
                Map.of(
                        "url", "https://orkes.io/content/blog/conductor-vs-alternatives",
                        "title", "Conductor vs Alternatives - Workflow Orchestration Comparison",
                        "snippet", "Compare Conductor with other workflow orchestration tools. Learn about features like task queuing, distributed execution, and fault tolerance."
                )
        );

        Map<String, String> topResult = results.get(0);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("topResult", topResult);
        result.getOutputData().put("totalFound", 2450);
        return result;
    }
}
