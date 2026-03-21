package milestonetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AtRiskWorker implements Worker {
    @Override public String getTaskDefName() { return "mst_at_risk"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [at_risk] Milestone " + task.getInputData().get("milestoneId") + " needs attention");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "escalate_to_lead"); return r;
    }
}
