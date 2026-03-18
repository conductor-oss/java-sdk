package dataaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Formats aggregate results into human-readable report lines.
 * Input: aggregates (map of key to {count, sum, avg, min, max})
 * Output: report (list of formatted strings)
 */
public class FormatReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agg_format_report";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Map<String, Object>> aggregates =
                (Map<String, Map<String, Object>>) task.getInputData().get("aggregates");
        if (aggregates == null) {
            aggregates = Map.of();
        }

        List<String> report = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : aggregates.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> stats = entry.getValue();
            int count = ((Number) stats.get("count")).intValue();
            double sum = ((Number) stats.get("sum")).doubleValue();
            double avg = ((Number) stats.get("avg")).doubleValue();
            double min = ((Number) stats.get("min")).doubleValue();
            double max = ((Number) stats.get("max")).doubleValue();
            String line = key + ": count=" + count + ", sum=" + sum + ", avg=" + avg
                    + ", min=" + min + ", max=" + max;
            report.add(line);
        }

        System.out.println("  [agg_format_report] Formatted " + report.size() + " report lines");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", report);
        return result;
    }
}
