package codereviewai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class StyleCheckWorker implements Worker {
    @Override public String getTaskDefName() { return "cra_style_check"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [style] Style analysis: 3 issues found");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("findings", List.of(
            Map.of("severity", "low", "file", "src/auth.js", "line", 12, "message", "Missing JSDoc"),
            Map.of("severity", "low", "file", "src/auth.js", "line", 30, "message", "Inconsistent quotes"),
            Map.of("severity", "low", "file", "src/api/users.js", "line", 1, "message", "Missing file header")));
        r.getOutputData().put("issueCount", 3);
        return r;
    }
}
