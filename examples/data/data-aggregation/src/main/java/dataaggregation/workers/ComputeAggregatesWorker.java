package dataaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes aggregate statistics (count, sum, avg, min, max) for each group
 * on a specified numeric field.
 * Input: groups (map of key to list of records), aggregateField (string)
 * Output: aggregates (map of key to {count, sum, avg, min, max})
 */
public class ComputeAggregatesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agg_compute_aggregates";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, List<Map<String, Object>>> groups =
                (Map<String, List<Map<String, Object>>>) task.getInputData().get("groups");
        if (groups == null) {
            groups = Map.of();
        }

        String aggregateField = (String) task.getInputData().get("aggregateField");
        if (aggregateField == null) {
            aggregateField = "value";
        }

        Map<String, Map<String, Object>> aggregates = new LinkedHashMap<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            String key = entry.getKey();
            List<Map<String, Object>> records = entry.getValue();

            int count = 0;
            double sum = 0.0;
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;

            for (Map<String, Object> record : records) {
                Object val = record.get(aggregateField);
                if (val != null) {
                    double num = ((Number) val).doubleValue();
                    count++;
                    sum += num;
                    if (num < min) min = num;
                    if (num > max) max = num;
                }
            }

            double avg = count > 0 ? sum / count : 0.0;
            if (count == 0) {
                min = 0.0;
                max = 0.0;
            }

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("count", count);
            stats.put("sum", sum);
            stats.put("avg", avg);
            stats.put("min", min);
            stats.put("max", max);
            aggregates.put(key, stats);
        }

        System.out.println("  [agg_compute_aggregates] Computed aggregates for " + aggregates.size() + " groups");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregates", aggregates);
        return result;
    }
}
