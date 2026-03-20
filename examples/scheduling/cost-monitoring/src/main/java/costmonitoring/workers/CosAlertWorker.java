package costmonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CosAlertWorker implements Worker {
    @Override public String getTaskDefName() { return "cos_alert_anomalies"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        double pct = 0; try { pct = Double.parseDouble(String.valueOf(task.getInputData().get("percentOfBudget"))); } catch (Exception ignored) {}
        boolean shouldAlert = pct > 80;
        System.out.println("  [alert] Budget at " + pct + "% - alert: " + shouldAlert);
        r.getOutputData().put("alertSent", shouldAlert);
        r.getOutputData().put("severity", pct > 90 ? "critical" : "warning");
        return r;
    }
}
