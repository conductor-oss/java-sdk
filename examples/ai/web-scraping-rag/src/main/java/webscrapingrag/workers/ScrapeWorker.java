package webscrapingrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that scrapes web pages and extracts content.
 * Returns demo pages with title, text, and word count.
 */
public class ScrapeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wsrag_scrape";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> urls = (List<String>) task.getInputData().get("urls");
        System.out.println("  [scrape] Fetching " + urls.size() + " URLs...");

        List<Map<String, Object>> pages = List.of(
                Map.of(
                        "url", urls.get(0),
                        "title", "Conductor Docs - Overview",
                        "text", "Orkes Conductor is a platform for orchestrating microservices, AI pipelines, and business workflows. It provides durable execution and full observability.",
                        "wordCount", 24
                ),
                Map.of(
                        "url", urls.get(1),
                        "title", "Conductor Docs - Workers",
                        "text", "Workers are the building blocks of Conductor workflows. Each worker handles a specific task type, polling for tasks and reporting results back to the server.",
                        "wordCount", 27
                )
        );

        for (Map<String, Object> p : pages) {
            System.out.println("    - " + p.get("url") + ": \"" + p.get("title") + "\" (" + p.get("wordCount") + " words)");
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("pages", pages);
        result.getOutputData().put("pageCount", pages.size());
        return result;
    }
}
