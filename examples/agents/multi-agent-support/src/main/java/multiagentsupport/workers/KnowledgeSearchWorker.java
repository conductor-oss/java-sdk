package multiagentsupport.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches the knowledge base for articles matching the ticket keywords.
 * Returns 3 relevant KB articles with relevance scores.
 */
public class KnowledgeSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cs_knowledge_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String subject = (String) task.getInputData().get("subject");
        String description = (String) task.getInputData().get("description");

        if (subject == null) subject = "";
        if (description == null) description = "";

        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) task.getInputData().get("keywords");
        if (keywords == null) keywords = List.of();

        System.out.println("  [cs_knowledge_search] Searching KB for: " + keywords);

        List<Map<String, Object>> articles = new ArrayList<>();

        Map<String, Object> article1 = new LinkedHashMap<>();
        article1.put("id", "KB-1001");
        article1.put("title", "Troubleshooting Common Application Errors");
        article1.put("relevance", 0.95);
        article1.put("excerpt", "When encountering application errors, first check the logs for stack traces and verify system requirements are met.");
        articles.add(article1);

        Map<String, Object> article2 = new LinkedHashMap<>();
        article2.put("id", "KB-1002");
        article2.put("title", "System Recovery and Restart Procedures");
        article2.put("relevance", 0.87);
        article2.put("excerpt", "Follow the standard recovery procedure: clear cache, restart services, and validate configuration files.");
        articles.add(article2);

        Map<String, Object> article3 = new LinkedHashMap<>();
        article3.put("id", "KB-1003");
        article3.put("title", "Known Issues and Workarounds");
        article3.put("relevance", 0.82);
        article3.put("excerpt", "This article covers known issues reported in the current release and their recommended workarounds.");
        articles.add(article3);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("kbArticles", articles);
        result.getOutputData().put("searchTime", "120ms");
        return result;
    }
}
