package bugtriage.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ClassifySeverityWorker implements Worker {
    @Override public String getTaskDefName() { return "btg_classify_severity"; }
    @Override public TaskResult execute(Task task) {
        String desc = ((String) task.getInputData().getOrDefault("description", "")).toLowerCase();
        String severity;
        if (desc.contains("crash") || desc.contains("data loss")) { severity = "critical"; }
        else if (desc.contains("broken") || desc.contains("error")) { severity = "high"; }
        else { severity = "low"; }
        System.out.println("  [classify] Severity: " + severity);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("severity", severity);
        return result;
    }
}
