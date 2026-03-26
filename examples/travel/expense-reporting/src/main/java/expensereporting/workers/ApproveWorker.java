package expensereporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "exr_approve"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [approve] Processing expense reporting step");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("approved", true); r.getOutputData().put("approvedBy", "MGR-90"); return r; } }
