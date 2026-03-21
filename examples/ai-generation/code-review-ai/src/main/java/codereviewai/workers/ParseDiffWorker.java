package codereviewai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class ParseDiffWorker implements Worker {
    @Override public String getTaskDefName() { return "cra_parse_diff"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [parse] Diff parsed from " + task.getInputData().get("prUrl"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("files", List.of("src/auth.js", "src/api/users.js", "src/utils/crypto.js"));
        r.getOutputData().put("linesChanged", 142); r.getOutputData().put("hunks", 8);
        return r;
    }
}
