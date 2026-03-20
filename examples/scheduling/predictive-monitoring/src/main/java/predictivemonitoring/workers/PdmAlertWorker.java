package predictivemonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class PdmAlertWorker implements Worker {
    @Override public String getTaskDefName() { return "pdm_alert"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        double likelihood = 0; try { likelihood = Double.parseDouble(String.valueOf(task.getInputData().get("breachLikelihood"))); } catch (Exception ignored) {}
        boolean shouldAlert = likelihood > 50;
        System.out.println("  [alert] Breach likelihood: " + likelihood + "% - alert: " + shouldAlert);
        r.getOutputData().put("alertSent", shouldAlert);
        return r;
    }
}
