package anomalydetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class AlertWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "anom_alert";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String severity = (String) task.getInputData().getOrDefault("severity", "info");
        String metricName = (String) task.getInputData().getOrDefault("metricName", "unknown");
        String classification = (String) task.getInputData().getOrDefault("classification", "unclassified");
        double zScore = toDouble(task.getInputData().getOrDefault("zScore", 0));
        double deviationPercent = toDouble(task.getInputData().getOrDefault("deviationPercent", 0));
        String recommendedAction = (String) task.getInputData().getOrDefault("recommendedAction", "investigate");
        boolean isAnomaly = Boolean.TRUE.equals(task.getInputData().get("isAnomaly"));

        Instant now = Instant.now();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(now);
        String alertId = "ANOM-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneOffset.UTC).format(now) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // Determine alert channel based on severity
        String channel;
        String priority;
        switch (severity) {
            case "critical":
                channel = "pagerduty";
                priority = "P1";
                break;
            case "warning":
                channel = "slack";
                priority = "P2";
                break;
            default:
                channel = "email";
                priority = "P3";
                break;
        }

        // Build alert message with real details
        String message = String.format(
                "[%s] %s anomaly detected on '%s': z-score=%.2f, deviation=%.1f%%, classification=%s. Action: %s",
                priority, severity.toUpperCase(), metricName, zScore, deviationPercent,
                classification, recommendedAction
        );

        boolean alerted = isAnomaly;
        if (!isAnomaly) {
            // No anomaly means no real alert needed, just log
            channel = "none";
            message = "No anomaly detected for '" + metricName + "' — no alert sent";
        }

        System.out.println("  [alert] " + (alerted ? "SENT" : "SKIPPED") + " via " + channel
                + ": " + message);

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("alerted", alerted);
        r.getOutputData().put("alertId", alertId);
        r.getOutputData().put("channel", channel);
        r.getOutputData().put("priority", priority);
        r.getOutputData().put("message", message);
        r.getOutputData().put("timestamp", timestamp);
        r.getOutputData().put("metricName", metricName);
        r.getOutputData().put("severity", severity);
        r.getOutputData().put("classification", classification);
        return r;
    }

    private static double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0.0; }
    }
}
