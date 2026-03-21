package toolusesequential.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Reads a web page.
 * Takes a url and title, returns the page content including sections and word count.
 */
public class ReadPageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ts_read_page";
    }

    @Override
    public TaskResult execute(Task task) {
        String url = (String) task.getInputData().get("url");
        if (url == null || url.isBlank()) {
            url = "https://example.com";
        }

        String title = (String) task.getInputData().get("title");
        if (title == null || title.isBlank()) {
            title = "Untitled Page";
        }

        System.out.println("  [ts_read_page] Reading page: " + url);

        List<Map<String, Object>> sections = List.of(
                Map.of(
                        "heading", "What is Conductor?",
                        "text", "Conductor is an open-source workflow orchestration engine originally developed at Netflix. It enables developers to define, execute, and manage complex workflows across distributed microservices with built-in fault tolerance and scalability."
                ),
                Map.of(
                        "heading", "Key Features",
                        "text", "Conductor provides task queuing, worker polling, workflow versioning, sub-workflows, error handling with retries, and a visual workflow designer. It supports both synchronous and asynchronous task execution."
                ),
                Map.of(
                        "heading", "Architecture",
                        "text", "The Conductor server acts as the central orchestrator, managing workflow state and task distribution. Workers poll for tasks, execute them, and report results back. This decoupled architecture allows independent scaling of workers."
                ),
                Map.of(
                        "heading", "Use Cases",
                        "text", "Common use cases include media processing pipelines, microservice orchestration, data processing workflows, CI/CD pipelines, order management, and AI/ML pipeline orchestration."
                )
        );

        Map<String, Object> content = Map.of(
                "title", title,
                "url", url,
                "wordCount", 1850,
                "sections", sections
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("content", content);
        result.getOutputData().put("statusCode", 200);
        result.getOutputData().put("loadTimeMs", 320);
        return result;
    }
}
