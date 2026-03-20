package contentmoderation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class AutoCheckWorker implements Worker {
    @Override public String getTaskDefName() { return "mod_auto_check"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [check] Processing " + task.getInputData().getOrDefault("confidence", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("confidence", 0.95);
        r.getOutputData().put("flagReasons", List.of());
        r.getOutputData().put("toxicityScore", 0.05);
        return r;
    }
}
