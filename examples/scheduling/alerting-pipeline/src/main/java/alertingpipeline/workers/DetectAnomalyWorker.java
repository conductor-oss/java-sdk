package alertingpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectAnomalyWorker implements Worker {
    @Override public String getTaskDefName() { return "alt_detect_anomaly"; }
    @Override public TaskResult execute(Task task) {
        double value = 0, threshold = 0;
        try { value = Double.parseDouble(String.valueOf(task.getInputData().get("currentValue"))); } catch (Exception ignored) {}
        try { threshold = Double.parseDouble(String.valueOf(task.getInputData().get("threshold"))); } catch (Exception ignored) {}
        boolean isAnomaly = value > threshold;
        System.out.println("  [detect] " + task.getInputData().get("metricName") + ": " + value + " vs threshold " + threshold + " - anomaly: " + isAnomaly);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("isAnomaly", isAnomaly);
        r.getOutputData().put("severity", isAnomaly ? "critical" : "info");
        r.getOutputData().put("anomalyScore", isAnomaly ? 0.92 : 0.1);
        r.getOutputData().put("deviation", value - threshold);
        return r;
    }
}
