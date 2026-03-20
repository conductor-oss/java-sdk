package stakeholderreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class CollectUpdatesWorker implements Worker {
    @Override public String getTaskDefName() { return "shr_collect_updates"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Gathering updates for " + task.getInputData().get("projectId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("updates", List.of(Map.of("team","Backend","status","on_track"),Map.of("team","Frontend","status","at_risk"),Map.of("team","QA","status","on_track"))); return r;
    }
}
