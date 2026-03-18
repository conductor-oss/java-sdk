package anomalydetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "anom_detect";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        Object meanObj = task.getInputData().get("mean");
        Object stdDevObj = task.getInputData().get("stdDev");
        Object latestObj = task.getInputData().get("latestValue");
        String sensitivity = (String) task.getInputData().getOrDefault("sensitivity", "medium");
        String metricName = (String) task.getInputData().getOrDefault("metricName", "unknown");

        if (meanObj == null || stdDevObj == null || latestObj == null) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Missing required inputs: mean, stdDev, and latestValue are all required");
            return r;
        }

        double mean = toDouble(meanObj);
        double stdDev = toDouble(stdDevObj);
        double latest = toDouble(latestObj);

        if (stdDev <= 0) {
            // Zero or negative stdDev means no variance; any deviation is notable
            r.setStatus(TaskResult.Status.COMPLETED);
            boolean isAnomaly = Math.abs(latest - mean) > 0.001;
            r.getOutputData().put("isAnomaly", isAnomaly);
            r.getOutputData().put("zScore", isAnomaly ? Double.MAX_VALUE : 0.0);
            r.getOutputData().put("deviation", latest - mean);
            r.getOutputData().put("method", "z-score");
            r.getOutputData().put("metricName", metricName);
            r.getOutputData().put("threshold", 0.0);
            System.out.println("  [detect] stdDev is zero; anomaly=" + isAnomaly);
            return r;
        }

        // Compute z-score
        double zScore = (latest - mean) / stdDev;
        double absZ = Math.abs(zScore);

        // Threshold depends on sensitivity
        double threshold = switch (sensitivity) {
            case "high" -> 2.0;
            case "low" -> 3.5;
            default -> 2.5;   // medium
        };

        boolean isAnomaly = absZ > threshold;
        double deviation = latest - mean;
        double deviationPercent = (mean != 0) ? Math.round((deviation / mean) * 10000.0) / 100.0 : 0.0;

        // Direction of anomaly
        String direction = "none";
        if (isAnomaly) {
            direction = zScore > 0 ? "above" : "below";
        }

        // Round z-score for output
        zScore = Math.round(zScore * 100.0) / 100.0;

        System.out.println("  [detect] metric='" + metricName + "' z=" + zScore
                + " threshold=" + threshold + " anomaly=" + isAnomaly
                + (isAnomaly ? " (" + direction + " baseline by " + deviationPercent + "%)" : ""));

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("isAnomaly", isAnomaly);
        r.getOutputData().put("zScore", zScore);
        r.getOutputData().put("deviation", Math.round(deviation * 100.0) / 100.0);
        r.getOutputData().put("deviationPercent", deviationPercent);
        r.getOutputData().put("direction", direction);
        r.getOutputData().put("threshold", threshold);
        r.getOutputData().put("method", "z-score");
        r.getOutputData().put("sensitivity", sensitivity);
        r.getOutputData().put("metricName", metricName);
        r.getOutputData().put("latestValue", latest);
        r.getOutputData().put("mean", mean);
        return r;
    }

    private static double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0.0; }
    }
}
