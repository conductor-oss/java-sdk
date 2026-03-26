package riskmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class HighWorker implements Worker {
    @Override public String getTaskDefName() { return "rkm_high"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [high] HIGH severity — immediate executive escalation");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "executive_escalation"); return r;
    }
}
