package multiagentresearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Writes the final research report — takes topic, synthesis, key insights, and source count,
 * and produces a title, executive summary, section count, and word count.
 */
public class WriteReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ra_write_report";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "research topic";
        }
        Map<String, Object> synthesis = (Map<String, Object>) task.getInputData().get("synthesis");
        List<String> keyInsights = (List<String>) task.getInputData().get("keyInsights");
        Object sourceCountObj = task.getInputData().get("sourceCount");

        int sourceCount = 0;
        if (sourceCountObj instanceof Number) {
            sourceCount = ((Number) sourceCountObj).intValue();
        }

        System.out.println("  [ra_write_report] Writing report on: " + topic);

        String title = "Research Report: " + topic;

        String executiveSummary = String.format(
                "This report synthesizes findings from %d sources across web, academic, and internal database searches. "
                + "The research examines %s, identifying key trends, challenges, and opportunities. "
                + "Analysis reveals significant productivity gains from AI-assisted development tools, "
                + "while highlighting ongoing concerns about code quality and the need for robust review processes.",
                sourceCount, topic);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("title", title);
        result.getOutputData().put("executiveSummary", executiveSummary);
        result.getOutputData().put("sections", 5);
        result.getOutputData().put("wordCount", 3200);
        return result;
    }
}
