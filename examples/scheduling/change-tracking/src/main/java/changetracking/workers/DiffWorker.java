package changetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class DiffWorker implements Worker {
    @Override public String getTaskDefName() { return "chg_diff"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [diff] Diffing " + task.getInputData().get("resourceId"));
        r.getOutputData().put("linesAdded", 145);
        r.getOutputData().put("linesRemoved", 32);
        r.getOutputData().put("filesChanged", 8);
        r.getOutputData().put("summary", "Added rate limiting middleware");
        return r;
    }
}
