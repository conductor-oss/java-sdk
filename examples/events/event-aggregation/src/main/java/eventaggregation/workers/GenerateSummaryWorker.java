package eventaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Generates a human-readable summary report from the aggregated metrics,
 * including highlights and a destination for the analytics pipeline.
 */
public class GenerateSummaryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eg_generate_summary";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> aggregation =
                (Map<String, Object>) task.getInputData().get("aggregation");
        if (aggregation == null) {
            aggregation = Map.of();
        }

        String windowId = (String) task.getInputData().get("windowId");
        if (windowId == null || windowId.isBlank()) {
            windowId = "win-default";
        }

        System.out.println("  [eg_generate_summary] Generating summary for window: " + windowId);

        List<String> highlights = List.of(
                "6 events processed",
                "Net revenue: $478.47",
                "Top product: Widget B (2 events)"
        );

        Map<String, Object> summary = Map.of(
                "windowId", windowId,
                "generatedAt", "2026-01-15T10:00:00Z",
                "metrics", aggregation,
                "highlights", highlights
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("destination", "analytics_pipeline");
        return result;
    }
}
