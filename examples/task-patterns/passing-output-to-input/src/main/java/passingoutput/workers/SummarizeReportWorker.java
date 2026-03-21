package passingoutput.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Summarizes a report using data from both previous tasks.
 * Demonstrates accessing outputs from multiple upstream task references.
 */
public class SummarizeReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "summarize_report";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> originalMetrics = (Map<String, Object>) task.getInputData().get("originalMetrics");
        Map<String, Object> enrichedInsights = (Map<String, Object>) task.getInputData().get("enrichedInsights");
        double growthRate = ((Number) task.getInputData().get("growthRate")).doubleValue();
        String region = (String) task.getInputData().get("region");
        String period = (String) task.getInputData().get("period");

        String healthScore = (String) enrichedInsights.get("healthScore");
        System.out.println("  [summary] Growth: " + growthRate + "%, Health: " + healthScore);

        String recommendation;
        if (growthRate > 10) {
            recommendation = "Expand operations — strong growth trajectory";
        } else if (growthRate > 0) {
            recommendation = "Maintain course — steady growth";
        } else {
            recommendation = "Review strategy — declining metrics";
        }

        int revenue = ((Number) originalMetrics.get("revenue")).intValue();
        String summary = region + " " + period + ": $" + revenue + " revenue, "
                + growthRate + "% growth, " + healthScore;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("recommendation", recommendation);

        return result;
    }
}
