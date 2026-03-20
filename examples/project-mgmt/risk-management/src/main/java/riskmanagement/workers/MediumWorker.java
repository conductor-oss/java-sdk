package riskmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class MediumWorker implements Worker {
    @Override public String getTaskDefName() { return "rkm_medium"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [medium] MEDIUM severity — team review required");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "team_review"); return r;
    }
}
