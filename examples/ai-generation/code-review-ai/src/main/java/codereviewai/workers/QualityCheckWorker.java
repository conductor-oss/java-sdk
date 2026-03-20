package codereviewai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class QualityCheckWorker implements Worker {
    @Override public String getTaskDefName() { return "cra_quality_check"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [quality] Code quality analysis: 2 issues found");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("findings", List.of(
            Map.of("severity", "medium", "file", "src/api/users.js", "line", 45, "message", "Function exceeds 50 lines"),
            Map.of("severity", "low", "file", "src/api/users.js", "line", 78, "message", "Unused variable")));
        r.getOutputData().put("issueCount", 2);
        return r;
    }
}
