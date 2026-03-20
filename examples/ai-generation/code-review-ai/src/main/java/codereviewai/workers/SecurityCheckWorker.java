package codereviewai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class SecurityCheckWorker implements Worker {
    @Override public String getTaskDefName() { return "cra_security_check"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [security] Security analysis: 1 issue found");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("findings", List.of(Map.of("severity", "high", "file", "src/utils/crypto.js", "line", 23, "message", "Weak hash algorithm (MD5)")));
        r.getOutputData().put("issueCount", 1);
        return r;
    }
}
