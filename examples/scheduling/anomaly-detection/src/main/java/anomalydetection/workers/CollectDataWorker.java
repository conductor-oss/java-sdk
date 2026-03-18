package anomalydetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CollectDataWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "anom_collect_data";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String metricName = (String) task.getInputData().getOrDefault("metricName", "unknown_metric");
        int lookbackHours = toInt(task.getInputData().getOrDefault("lookbackHours", 24));
        String sensitivity = (String) task.getInputData().getOrDefault("sensitivity", "medium");

        if (lookbackHours <= 0 || lookbackHours > 720) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("lookbackHours must be between 1 and 720, got: " + lookbackHours);
            return r;
        }

        // Generate time series data points: one per 2-minute interval for the lookback period
        int intervalMinutes = 2;
        int dataPointCount = (lookbackHours * 60) / intervalMinutes;

        // Determine baseline characteristics from metric name
        double baseMean;
        double baseStdDev;
        if (metricName.contains("latency")) {
            baseMean = 200.0;  // ms
            baseStdDev = 30.0;
        } else if (metricName.contains("cpu")) {
            baseMean = 45.0;   // percent
            baseStdDev = 10.0;
        } else if (metricName.contains("memory")) {
            baseMean = 60.0;   // percent
            baseStdDev = 8.0;
        } else if (metricName.contains("error")) {
            baseMean = 2.0;    // count per interval
            baseStdDev = 1.5;
        } else {
            baseMean = 100.0;
            baseStdDev = 15.0;
        }

        // Adjust noise scale by sensitivity
        double noiseScale = switch (sensitivity) {
            case "high" -> 0.8;
            case "low" -> 1.5;
            default -> 1.0;
        };

        // Generate synthetic time series
        Random rng = new Random(metricName.hashCode() + lookbackHours);
        List<Double> dataPoints = new ArrayList<>(dataPointCount);
        Instant now = Instant.now();

        for (int i = 0; i < dataPointCount; i++) {
            double noise = rng.nextGaussian() * baseStdDev * noiseScale;
            double value = baseMean + noise;
            // Add occasional spikes (last 5% of points might have anomalies)
            if (i >= dataPointCount * 0.95 && rng.nextDouble() < 0.1) {
                value = baseMean + (baseStdDev * (4 + rng.nextDouble() * 2));
            }
            dataPoints.add(Math.round(value * 100.0) / 100.0);
        }

        double latestValue = dataPoints.get(dataPoints.size() - 1);
        Instant collectStart = now.minus(lookbackHours, ChronoUnit.HOURS);

        System.out.println("  [collect] Collected " + dataPointCount + " data points for '"
                + metricName + "' over " + lookbackHours + "h (latest=" + latestValue + ")");

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("metricName", metricName);
        r.getOutputData().put("dataPoints", dataPoints);
        r.getOutputData().put("dataPointCount", dataPointCount);
        r.getOutputData().put("latestValue", latestValue);
        r.getOutputData().put("lookbackHours", lookbackHours);
        r.getOutputData().put("sensitivity", sensitivity);
        r.getOutputData().put("collectStart", collectStart.toString());
        r.getOutputData().put("collectEnd", now.toString());
        return r;
    }

    private static int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception e) { return 24; }
    }
}
