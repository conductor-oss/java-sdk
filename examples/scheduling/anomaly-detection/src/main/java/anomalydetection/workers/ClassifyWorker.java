package anomalydetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ClassifyWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "anom_classify";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        Object zScoreObj = task.getInputData().get("zScore");
        boolean isAnomaly = Boolean.TRUE.equals(task.getInputData().get("isAnomaly"));
        String metricName = (String) task.getInputData().getOrDefault("metricName", "unknown");
        String direction = (String) task.getInputData().getOrDefault("direction", "none");
        double deviationPercent = toDouble(task.getInputData().getOrDefault("deviationPercent", 0));

        double zScore = 0;
        if (zScoreObj != null) {
            zScore = toDouble(zScoreObj);
        }
        double absZ = Math.abs(zScore);

        // Classify by severity based on z-score magnitude
        String classification;
        String severity;
        String recommendedAction;

        if (!isAnomaly) {
            classification = "normal";
            severity = "none";
            recommendedAction = "no action required";
        } else if (absZ > 5) {
            classification = "extreme_spike";
            severity = "critical";
            recommendedAction = "immediate investigation and potential rollback";
        } else if (absZ > 4) {
            classification = "spike";
            severity = "critical";
            recommendedAction = "page on-call engineer immediately";
        } else if (absZ > 3) {
            classification = "significant";
            severity = "warning";
            recommendedAction = "investigate within 30 minutes";
        } else {
            classification = "moderate";
            severity = "info";
            recommendedAction = "monitor closely, investigate if recurring";
        }

        // Further classify by direction
        String pattern;
        if ("above".equals(direction)) {
            pattern = absZ > 4 ? "sudden_spike" : "gradual_increase";
        } else if ("below".equals(direction)) {
            pattern = absZ > 4 ? "sudden_drop" : "gradual_decrease";
        } else {
            pattern = "within_bounds";
        }

        System.out.println("  [classify] metric='" + metricName + "' classification=" + classification
                + " severity=" + severity + " pattern=" + pattern
                + " (z=" + zScore + ", deviation=" + deviationPercent + "%)");

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("classification", classification);
        r.getOutputData().put("severity", severity);
        r.getOutputData().put("pattern", pattern);
        r.getOutputData().put("recommendedAction", recommendedAction);
        r.getOutputData().put("zScore", zScore);
        r.getOutputData().put("deviationPercent", deviationPercent);
        r.getOutputData().put("metricName", metricName);
        r.getOutputData().put("isAnomaly", isAnomaly);
        return r;
    }

    private static double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0.0; }
    }
}
