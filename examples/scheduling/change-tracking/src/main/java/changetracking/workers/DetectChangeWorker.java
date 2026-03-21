package changetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class DetectChangeWorker implements Worker {
    @Override public String getTaskDefName() { return "chg_detect_change"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [detect] Detected change in " + task.getInputData().get("resourceType") + " " + task.getInputData().get("resourceId"));
        r.getOutputData().put("changeDetected", true);
        r.getOutputData().put("previousVersion", "v2.4.0");
        r.getOutputData().put("currentVersion", "v2.5.0");
        return r;
    }
}
