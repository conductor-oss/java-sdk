package simpleplussystem.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.Map;

/**
 * SIMPLE worker that generates a visual report (chart) from stats.
 *
 * In a real system this would call a charting service or generate an image.
 * Here it generates chart generation and returns a URL.
 */
public class GenerateVisualReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "generate_visual_report";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> stats = (Map<String, Object>) task.getInputData().get("stats");
        String storeId = (String) task.getInputData().get("storeId");

        System.out.println("  [SIMPLE] Generating chart for store " + storeId);

        if (stats != null) {
            Object totalRevenue = stats.get("totalRevenue");
            Object avgOrderValue = stats.get("avgOrderValue");
            System.out.println("    Revenue: $" + totalRevenue + ", Avg: $" + avgOrderValue);

            // Perform a bar chart in the console
            if (totalRevenue instanceof Number) {
                int bars = ((Number) totalRevenue).intValue() / 50;
                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < bars; i++) bar.append('\u2588');
                System.out.println("    " + bar + " Total");
            }
        }

        String chartUrl = "https://charts.example.com/" + storeId + "/revenue.png";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("chartUrl", chartUrl);
        result.getOutputData().put("generatedAt", Instant.now().toString());
        return result;
    }
}
