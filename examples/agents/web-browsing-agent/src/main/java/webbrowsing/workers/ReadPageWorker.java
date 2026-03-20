package webbrowsing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the content of selected pages. Performs page loading and content extraction.
 * Returns page contents with url, title, content, word count, and load time.
 */
public class ReadPageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wb_read_page";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> selectedPages =
                (List<Map<String, Object>>) task.getInputData().get("selectedPages");

        if (selectedPages == null || selectedPages.isEmpty()) {
            selectedPages = List.of();
        }

        System.out.println("  [wb_read_page] Reading " + selectedPages.size() + " pages");

        List<Map<String, Object>> pageContents = new ArrayList<>();
        int totalWordsRead = 0;

        for (int i = 0; i < selectedPages.size(); i++) {
            Map<String, Object> page = selectedPages.get(i);
            String url = (String) page.getOrDefault("url", "https://example.com/page" + i);
            String title = (String) page.getOrDefault("title", "Page " + (i + 1));

            String content = generatePageContent(title, i);
            int wordCount = content.split("\\s+").length;
            int loadTimeMs = 120 + (i * 45);

            pageContents.add(Map.of(
                    "url", url,
                    "title", title,
                    "content", content,
                    "wordCount", wordCount,
                    "loadTimeMs", loadTimeMs
            ));

            totalWordsRead += wordCount;
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("pageContents", pageContents);
        result.getOutputData().put("totalWordsRead", totalWordsRead);
        return result;
    }

    private String generatePageContent(String title, int index) {
        return switch (index) {
            case 0 -> "Conductor is a workflow orchestration engine originally developed at Netflix. "
                    + "It provides features such as task scheduling, workflow versioning, error handling, "
                    + "and distributed task execution. Conductor supports both simple and complex workflows "
                    + "with conditional branching, sub-workflows, and dynamic task allocation. "
                    + "It is designed for microservices architectures and can handle millions of concurrent workflows.";
            case 1 -> "Key features of Conductor include a powerful visual workflow designer, "
                    + "built-in rate limiting and throttling, comprehensive REST APIs, and support for "
                    + "multiple programming languages through polyglot workers. Conductor also provides "
                    + "workflow metrics, task-level retries with configurable policies, and integration "
                    + "with popular monitoring tools for production observability.";
            case 2 -> "In production environments, Conductor offers horizontal scaling capabilities, "
                    + "high availability through clustered deployments, and persistent workflow state "
                    + "storage. Organizations use Conductor to orchestrate data pipelines, manage "
                    + "order fulfillment processes, and coordinate distributed system operations. "
                    + "The platform supports event-driven architectures and webhook integrations.";
            default -> "Conductor workflow engine provides robust orchestration capabilities "
                    + "for distributed systems. It enables teams to define, execute, and monitor "
                    + "complex business processes as code with full visibility and control.";
        };
    }
}
