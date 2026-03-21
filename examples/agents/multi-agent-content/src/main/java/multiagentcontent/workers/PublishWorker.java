package multiagentcontent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Publish worker — publishes the final article with metadata.
 * Takes finalArticle, metadata, and seoScore; returns url, publishedAt
 * (fixed timestamp), and status="live".
 */
public class PublishWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cc_publish";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String finalArticle = (String) task.getInputData().get("finalArticle");
        Map<String, Object> metadata = (Map<String, Object>) task.getInputData().get("metadata");
        Object seoScore = task.getInputData().get("seoScore");

        String title = "article";
        if (metadata != null && metadata.get("title") != null) {
            title = ((String) metadata.get("title"))
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");
        }

        System.out.println("  [cc_publish] Publishing article: " + title + " (SEO score: " + seoScore + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("url", "https://content.example.com/articles/" + title);
        result.getOutputData().put("publishedAt", "2025-01-15T10:30:00Z");
        result.getOutputData().put("status", "live");
        return result;
    }
}
