package personalization.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class SelectContentWorker implements Worker {
    @Override public String getTaskDefName() { return "per_select_content"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [select] Processing " + task.getInputData().getOrDefault("candidates", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("candidates", List.of());
        r.getOutputData().put("id", "ART-101");
        r.getOutputData().put("title", "Kubernetes Best Practices");
        r.getOutputData().put("score", 0.92);
        return r;
    }
}
