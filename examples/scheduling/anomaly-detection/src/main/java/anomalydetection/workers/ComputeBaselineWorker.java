package anomalydetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComputeBaselineWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "anom_compute_baseline";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        Object dataObj = task.getInputData().get("dataPoints");
        String metricName = (String) task.getInputData().getOrDefault("metricName", "unknown");

        if (dataObj == null) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("No dataPoints provided");
            return r;
        }

        List<Double> dataPoints = new ArrayList<>();
        if (dataObj instanceof List) {
            for (Object v : (List<?>) dataObj) {
                dataPoints.add(toDouble(v));
            }
        } else {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("dataPoints must be a list");
            return r;
        }

        if (dataPoints.size() < 2) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Need at least 2 data points to compute baseline, got " + dataPoints.size());
            return r;
        }

        // Compute mean
        double sum = 0.0;
        for (double v : dataPoints) {
            sum += v;
        }
        double mean = sum / dataPoints.size();

        // Compute standard deviation
        double varianceSum = 0.0;
        for (double v : dataPoints) {
            varianceSum += (v - mean) * (v - mean);
        }
        double stdDev = Math.sqrt(varianceSum / dataPoints.size());

        // Compute median
        List<Double> sorted = new ArrayList<>(dataPoints);
        Collections.sort(sorted);
        double median;
        int n = sorted.size();
        if (n % 2 == 0) {
            median = (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        } else {
            median = sorted.get(n / 2);
        }

        // Compute min and max
        double min = sorted.get(0);
        double max = sorted.get(sorted.size() - 1);

        // Compute percentiles (p5 and p95)
        double p5 = sorted.get((int) (sorted.size() * 0.05));
        double p95 = sorted.get((int) (sorted.size() * 0.95));

        // Round for readability
        mean = Math.round(mean * 100.0) / 100.0;
        stdDev = Math.round(stdDev * 100.0) / 100.0;
        median = Math.round(median * 100.0) / 100.0;

        System.out.println("  [baseline] Computed baseline from " + dataPoints.size()
                + " points for '" + metricName + "': mean=" + mean + ", stdDev=" + stdDev + ", median=" + median);

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("mean", mean);
        r.getOutputData().put("stdDev", stdDev);
        r.getOutputData().put("median", median);
        r.getOutputData().put("min", Math.round(min * 100.0) / 100.0);
        r.getOutputData().put("max", Math.round(max * 100.0) / 100.0);
        r.getOutputData().put("p5", Math.round(p5 * 100.0) / 100.0);
        r.getOutputData().put("p95", Math.round(p95 * 100.0) / 100.0);
        r.getOutputData().put("sampleSize", dataPoints.size());
        r.getOutputData().put("metricName", metricName);
        return r;
    }

    private static double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0.0; }
    }
}
