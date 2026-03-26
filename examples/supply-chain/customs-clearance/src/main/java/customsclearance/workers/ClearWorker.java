package customsclearance.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ClearWorker implements Worker {
    @Override public String getTaskDefName() { return "cst_clear"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [clear] " + task.getInputData().get("declarationId") + " cleared — duty paid: $" + task.getInputData().get("dutyAmount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("clearanceId", "CLR-666-001"); r.getOutputData().put("cleared", true); return r;
    }
}
