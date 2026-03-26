package expensereporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ReimburseWorker implements Worker {
    @Override public String getTaskDefName() { return "exr_reimburse"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [reimburse] Processing expense reporting step");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reimbursed", true); r.getOutputData().put("paymentDate", "2024-04-01"); r.getOutputData().put("method", "direct-deposit"); return r; } }
