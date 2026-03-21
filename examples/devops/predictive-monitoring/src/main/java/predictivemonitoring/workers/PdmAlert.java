package predictivemonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PdmAlert implements Worker {

    @Override public String getTaskDefName() { return "pdm_alert"; }

    @Override
    public TaskResult execute(Task task) {
        String metricName = (String) task.getInputData().get("metricName");
        double likelihood = toDouble(task.getInputData().get("breachLikelihood"), 0);
        Object predictedPeak = task.getInputData().get("predictedPeak");
        boolean shouldAlert = likelihood > 50;

        System.out.println("[pdm_alert] Breach likelihood for " + metricName + ": " + likelihood + "% (peak: " + predictedPeak + ") -- alert: " + shouldAlert);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("alertSent", shouldAlert);
        result.getOutputData().put("severity", likelihood > 80 ? "critical" : "warning");
        result.getOutputData().put("message", metricName + " predicted to peak at " + predictedPeak);
        return result;
    }

    private double toDouble(Object val, double defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return defaultVal; }
    }
}
