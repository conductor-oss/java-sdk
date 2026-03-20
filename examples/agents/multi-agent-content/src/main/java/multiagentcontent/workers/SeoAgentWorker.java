package multiagentcontent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * SEO agent — optimizes an article draft for search engines.
 * Takes draft and topic; returns optimizedArticle, 4 suggestions, and seoScore=87.
 */
public class SeoAgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cc_seo_agent";
    }

    @Override
    public TaskResult execute(Task task) {
        String draft = (String) task.getInputData().get("draft");
        if (draft == null || draft.isBlank()) {
            draft = "";
        }

        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "general topic";
        }

        System.out.println("  [cc_seo_agent] Optimizing SEO for topic: " + topic);

        String optimizedArticle = draft
                + "\n\n<!-- SEO optimized: meta description, keyword density adjusted, "
                + "internal links added for " + topic + " -->\n";

        List<String> suggestions = List.of(
                "Add meta description targeting '" + topic + "' keyword",
                "Increase keyword density for '" + topic + "' from 1.2% to 2.0%",
                "Add 3 internal links to related " + topic + " content",
                "Include alt text for all images referencing " + topic
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("optimizedArticle", optimizedArticle);
        result.getOutputData().put("suggestions", suggestions);
        result.getOutputData().put("seoScore", 87);
        return result;
    }
}
