package riskmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AssessWorker implements Worker {
    @Override public String getTaskDefName() { return "rkm_assess"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [assess] Assessing risk severity");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("severity", "high"); r.getOutputData().put("probability", 0.7); r.getOutputData().put("impact", "critical"); return r;
    }
}
