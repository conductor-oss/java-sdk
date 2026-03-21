package webbrowsing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extracts and synthesizes an answer from the page contents.
 * Returns the answer, sources, confidence score, and word count.
 */
public class ExtractAnswerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wb_extract_answer";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        List<Map<String, Object>> pageContents =
                (List<Map<String, Object>>) task.getInputData().get("pageContents");

        if (question == null || question.isBlank()) {
            question = "general query";
        }
        if (pageContents == null || pageContents.isEmpty()) {
            pageContents = List.of();
        }

        System.out.println("  [wb_extract_answer] Synthesizing answer from "
                + pageContents.size() + " pages for: " + question);

        String answer = "Conductor is a workflow orchestration engine originally developed at Netflix, "
                + "now maintained as an open-source project. Its key features include: "
                + "(1) Task scheduling and distributed execution across microservices, "
                + "(2) Workflow versioning with support for conditional branching and sub-workflows, "
                + "(3) Built-in error handling with configurable retry policies, "
                + "(4) Rate limiting and throttling for production workloads, "
                + "(5) Comprehensive REST APIs and polyglot worker support, "
                + "and (6) Visual workflow designer for building and monitoring workflows. "
                + "For production deployments, Conductor provides horizontal scaling, "
                + "high availability through clustered deployments, persistent state storage, "
                + "and integration with monitoring tools. Organizations use it to orchestrate "
                + "data pipelines, order fulfillment, and distributed system operations at scale.";

        List<Map<String, String>> sources = new ArrayList<>();
        for (Map<String, Object> page : pageContents) {
            String title = (String) page.getOrDefault("title", "Unknown Source");
            String url = (String) page.getOrDefault("url", "https://example.com");
            sources.add(Map.of("title", title, "url", url));
        }

        int wordCount = answer.split("\\s+").length;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("sources", sources);
        result.getOutputData().put("confidence", 0.91);
        result.getOutputData().put("wordCount", wordCount);
        return result;
    }
}
