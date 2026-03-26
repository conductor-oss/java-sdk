package featureengineering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Min-max normalizes features to [0,1] range.
 * Input: transformedFeatures
 * Output: normalized, normalizedCount, stats
 */
public class NormalizeFeaturesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fe_normalize_features";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> features = (List<Map<String, Object>>) task.getInputData().get("transformedFeatures");
        if (features == null || features.isEmpty()) {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("normalized", List.of());
            result.getOutputData().put("normalizedCount", 0);
            return result;
        }

        Set<String> keys = features.get(0).keySet();
        Map<String, double[]> stats = new LinkedHashMap<>();
        for (String k : keys) {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (Map<String, Object> f : features) {
                double v = toDouble(f.get(k));
                if (v < min) min = v;
                if (v > max) max = v;
            }
            stats.put(k, new double[]{min, max});
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> f : features) {
            Map<String, Object> n = new LinkedHashMap<>();
            for (String k : keys) {
                double[] s = stats.get(k);
                double range = s[1] - s[0];
                double v = toDouble(f.get(k));
                n.put(k, range > 0 ? Math.round(((v - s[0]) / range) * 10000.0) / 10000.0 : 0.0);
            }
            normalized.add(n);
        }

        Map<String, Object> statsOutput = new LinkedHashMap<>();
        for (Map.Entry<String, double[]> e : stats.entrySet()) {
            statsOutput.put(e.getKey(), Map.of("min", e.getValue()[0], "max", e.getValue()[1]));
        }

        System.out.println("  [normalize] Min-max normalized " + keys.size() + " features across " + normalized.size() + " records");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("normalized", normalized);
        result.getOutputData().put("normalizedCount", keys.size());
        result.getOutputData().put("stats", statsOutput);
        return result;
    }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 0.0;
    }
}
