package alertingpipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EvaluateRulesWorker implements Worker {
    @Override public String getTaskDefName() { return "alt_evaluate_rules"; }
    @Override public TaskResult execute(Task task) {
        String severity = String.valueOf(task.getInputData().get("severity"));
        boolean shouldFire = "critical".equals(severity) || "warning".equals(severity);
        System.out.println("  [evaluate] Evaluating rules for " + task.getInputData().get("metricName") + " (severity: " + severity + ")");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", shouldFire ? "fire" : "suppress");
        r.getOutputData().put("channel", "pagerduty");
        r.getOutputData().put("message", task.getInputData().get("metricName") + " exceeded threshold");
        r.getOutputData().put("suppressReason", "Below alert threshold");
        return r;
    }
}
