package projectkickoff.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class CreatePlanWorker implements Worker {
    @Override public String getTaskDefName() { return "pkf_create_plan"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [plan] Creating project plan");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("plan", Map.of("phases",List.of("Discovery","Design","Build","Launch"),"milestones",4)); return r;
    }
}
