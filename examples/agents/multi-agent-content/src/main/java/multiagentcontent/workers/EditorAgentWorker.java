package multiagentcontent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Editor agent — polishes the SEO-optimized article, computes metadata, and
 * determines readability grade.
 *
 * Takes article and seoSuggestions; returns polishedArticle, wordCount,
 * metadata (title, author, tags, readTime), and readabilityGrade=8.2.
 *
 * readTime is computed deterministically: ceil(wordCount / 200) + " min".
 */
public class EditorAgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cc_editor_agent";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String article = (String) task.getInputData().get("article");
        if (article == null || article.isBlank()) {
            article = "";
        }

        List<String> seoSuggestions = (List<String>) task.getInputData().get("seoSuggestions");

        System.out.println("  [cc_editor_agent] Polishing article...");

        StringBuilder polished = new StringBuilder();
        polished.append(article);

        if (seoSuggestions != null && !seoSuggestions.isEmpty()) {
            polished.append("\n\n<!-- Editor notes: applied SEO suggestions —");
            for (String suggestion : seoSuggestions) {
                polished.append(" ").append(suggestion).append(";");
            }
            polished.append(" -->\n");
        }

        String polishedArticle = polished.toString();
        int wordCount = polishedArticle.split("\\s+").length;

        // readTime: ceil(wordCount / 200) + " min"
        int readTimeMinutes = (int) Math.ceil(wordCount / 200.0);
        String readTime = readTimeMinutes + " min";

        // Extract title from the article (first heading) or generate one
        String title = "Comprehensive Guide";
        if (article.startsWith("# ")) {
            int newline = article.indexOf('\n');
            if (newline > 2) {
                title = article.substring(2, newline).trim();
            }
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", title);
        metadata.put("author", "Content Creation Pipeline");
        metadata.put("tags", List.of("technology", "innovation", "best-practices"));
        metadata.put("readTime", readTime);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("polishedArticle", polishedArticle);
        result.getOutputData().put("wordCount", wordCount);
        result.getOutputData().put("metadata", metadata);
        result.getOutputData().put("readabilityGrade", 8.2);
        return result;
    }
}
