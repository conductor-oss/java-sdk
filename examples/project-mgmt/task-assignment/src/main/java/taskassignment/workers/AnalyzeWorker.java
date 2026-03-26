package taskassignment.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class AnalyzeWorker implements Worker {
    @Override public String getTaskDefName() { return "tas_analyze"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [analyze] Analyzing task: " + task.getInputData().get("taskTitle"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("skills", List.of("JavaScript","React")); r.getOutputData().put("complexity", "medium"); return r;
    }
}
