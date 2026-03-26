package projectkickoff.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class DefineScopeWorker implements Worker {
    @Override public String getTaskDefName() { return "pkf_define_scope"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [scope] Defining scope for " + task.getInputData().get("projectName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("scope", Map.of("objectives",List.of("Deliver MVP","User testing"),"deliverables",5,"timeline","12 weeks")); return r;
    }
}
