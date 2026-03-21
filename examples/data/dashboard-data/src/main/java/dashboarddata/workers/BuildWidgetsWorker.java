package dashboarddata.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds dashboard widget configurations from KPIs and metrics.
 * Input: kpis (list), metrics (map)
 * Output: widgets (list), widgetCount
 */
public class BuildWidgetsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dh_build_widgets";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> kpis =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("kpis", List.of());

        List<Map<String, Object>> widgets = List.of(
                Map.of("id", "w1", "type", "kpi_cards", "title", "Key Performance Indicators", "data", kpis),
                Map.of("id", "w2", "type", "line_chart", "title", "User Activity Trend", "dataPoints", 24),
                Map.of("id", "w3", "type", "bar_chart", "title", "Revenue by Hour", "dataPoints", 24),
                Map.of("id", "w4", "type", "gauge", "title", "System Health", "value", 98.2),
                Map.of("id", "w5", "type", "table", "title", "Top Pages", "rows", 10),
                Map.of("id", "w6", "type", "pie_chart", "title", "Traffic Sources", "segments", 5)
        );

        String types = widgets.stream().map(w -> (String) w.get("type")).collect(Collectors.joining(", "));
        System.out.println("  [widgets] Built " + widgets.size() + " dashboard widgets: " + types);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("widgets", widgets);
        result.getOutputData().put("widgetCount", widgets.size());
        return result;
    }
}
