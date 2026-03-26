package multiagentcontent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Writer agent — composes an article draft from research facts and sources.
 * Takes topic, facts, sources, and wordCount; returns article text and draftWordCount.
 */
public class WriterAgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cc_writer_agent";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "general topic";
        }

        List<String> facts = (List<String>) task.getInputData().get("facts");
        List<String> sources = (List<String>) task.getInputData().get("sources");

        Object wordCountObj = task.getInputData().get("wordCount");
        int wordCount = 800;
        if (wordCountObj instanceof Number) {
            wordCount = ((Number) wordCountObj).intValue();
        } else if (wordCountObj instanceof String) {
            try {
                wordCount = Integer.parseInt((String) wordCountObj);
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }

        System.out.println("  [cc_writer_agent] Writing article on: " + topic + " (target: " + wordCount + " words)");

        StringBuilder article = new StringBuilder();
        article.append("# ").append(topic).append(": A Comprehensive Guide\n\n");
        article.append("In today's rapidly evolving landscape, ").append(topic)
                .append(" has emerged as a critical area of focus for professionals and organizations alike. ")
                .append("This article explores the key developments, insights, and practical implications.\n\n");
        article.append("## Key Findings\n\n");

        if (facts != null) {
            for (String fact : facts) {
                article.append("- ").append(fact).append("\n");
            }
        }

        article.append("\n## Analysis\n\n");
        article.append("The data clearly shows that ").append(topic)
                .append(" is not just a passing trend but a fundamental shift in how we approach modern challenges. ")
                .append("Organizations that embrace this shift early stand to gain significant competitive advantages. ")
                .append("The evidence suggests a strong correlation between early adoption and measurable business outcomes.\n\n");
        article.append("## Practical Implications\n\n");
        article.append("For practitioners looking to leverage ").append(topic)
                .append(", the path forward involves strategic planning, investment in capabilities, and a commitment to continuous learning. ")
                .append("The most successful implementations share common characteristics: strong leadership support, ")
                .append("clear metrics for success, and an iterative approach to deployment.\n\n");
        article.append("## Conclusion\n\n");
        article.append("As we look ahead, ").append(topic)
                .append(" will continue to shape industries and redefine best practices. ")
                .append("The organizations that invest wisely today will be best positioned to thrive tomorrow.\n\n");

        if (sources != null) {
            article.append("## References\n\n");
            for (String source : sources) {
                article.append("- ").append(source).append("\n");
            }
        }

        String articleText = article.toString();
        int draftWordCount = articleText.split("\\s+").length;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("article", articleText);
        result.getOutputData().put("draftWordCount", draftWordCount);
        return result;
    }
}
