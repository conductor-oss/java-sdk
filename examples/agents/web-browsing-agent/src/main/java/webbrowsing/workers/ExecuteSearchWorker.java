package webbrowsing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Executes search queries and returns search results.
 * Returns a list of results with url, title, snippet, and relevance score.
 */
public class ExecuteSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wb_execute_search";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<String> searchQueries = (List<String>) task.getInputData().get("searchQueries");
        String searchEngine = (String) task.getInputData().get("searchEngine");

        if (searchQueries == null || searchQueries.isEmpty()) {
            searchQueries = List.of("Conductor workflow engine");
        }
        if (searchEngine == null || searchEngine.isBlank()) {
            searchEngine = "google";
        }

        System.out.println("  [wb_execute_search] Executing " + searchQueries.size()
                + " queries on " + searchEngine);

        List<Map<String, Object>> results = List.of(
                Map.of(
                        "url", "https://conductor.netflix.com/overview",
                        "title", "Conductor Overview - Netflix OSS",
                        "snippet", "Conductor is a workflow orchestration engine that runs in the cloud. It enables teams to build stateful, distributed workflows.",
                        "relevance", 0.95
                ),
                Map.of(
                        "url", "https://orkes.io/conductor-features",
                        "title", "Key Features of Conductor - Orkes",
                        "snippet", "Explore the key features of Conductor including task scheduling, error handling, rate limiting, and workflow versioning.",
                        "relevance", 0.92
                ),
                Map.of(
                        "url", "https://docs.conductor.io/production-guide",
                        "title", "Production Deployment Guide - Conductor Docs",
                        "snippet", "Learn how to deploy Conductor in production with high availability, horizontal scaling, and monitoring best practices.",
                        "relevance", 0.88
                ),
                Map.of(
                        "url", "https://engineering.blog/conductor-at-scale",
                        "title", "Running Conductor at Scale - Engineering Blog",
                        "snippet", "How we run Conductor to orchestrate millions of workflows daily with sub-second latency and 99.99% uptime.",
                        "relevance", 0.85
                ),
                Map.of(
                        "url", "https://community.conductor.io/comparison",
                        "title", "Conductor vs Other Workflow Engines",
                        "snippet", "A detailed comparison of Conductor with other workflow orchestration tools, covering features, performance, and ecosystem.",
                        "relevance", 0.80
                )
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("totalResults", results.size());
        return result;
    }
}
