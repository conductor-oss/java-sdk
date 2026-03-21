package stakeholderreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class DistributeWorker implements Worker {
    @Override public String getTaskDefName() { return "shr_distribute"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [distribute] Report sent to stakeholders for " + task.getInputData().get("projectId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("distributed", Map.of("recipients",5,"channels",List.of("email","slack"),"sentAt","2026-03-08T10:00:00Z")); return r;
    }
}
