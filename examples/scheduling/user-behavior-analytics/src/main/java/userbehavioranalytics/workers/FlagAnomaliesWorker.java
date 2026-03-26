package userbehavioranalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class FlagAnomaliesWorker implements Worker {
    @Override public String getTaskDefName() { return "uba_flag_anomalies"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        double riskScore = 0, threshold = 70;
        try { riskScore = Double.parseDouble(String.valueOf(task.getInputData().get("riskScore"))); } catch (Exception ignored) {}
        try { threshold = Double.parseDouble(String.valueOf(task.getInputData().get("riskThreshold"))); } catch (Exception ignored) {}
        boolean flagged = riskScore >= threshold;
        System.out.println("  [flag] User " + task.getInputData().get("userId") + ": risk " + riskScore + " vs " + threshold + " - flagged: " + flagged);
        r.getOutputData().put("flagged", flagged);
        r.getOutputData().put("alertSent", flagged);
        return r;
    }
}
