package changerequest.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class VerifyWorker implements Worker {
    @Override public String getTaskDefName() { return "chr_verify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [verify] Verifying change " + task.getInputData().get("changeId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("verification", Map.of("verified",true,"testsPass",true,"status","CLOSED")); return r;
    }
}
