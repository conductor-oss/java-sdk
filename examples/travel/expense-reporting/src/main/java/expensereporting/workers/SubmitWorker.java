package expensereporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "exr_submit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Processing expense reporting step");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reportId", "EXP-543"); r.getOutputData().put("submitted", true); return r; } }
