package codegeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class GenerateCodeWorker implements Worker {
    @Override public String getTaskDefName() { return "cdg_generate_code"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [generate] Code generated in " + task.getInputData().get("language") + " using " + task.getInputData().get("framework"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("code", "// Generated REST API\napp.post('/users', auth, async (req, res) => { ... });");
        r.getOutputData().put("linesOfCode", 85);
        r.getOutputData().put("testCases", List.of(Map.of("name", "create_user", "input", Map.of("email", "test@ex.com"), "expected", 201)));
        r.getOutputData().put("filesGenerated", List.of("routes/users.js", "routes/orders.js"));
        return r;
    }
}
