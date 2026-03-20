package personalization.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CollectProfileWorker implements Worker {
    @Override public String getTaskDefName() { return "per_collect_profile"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [profile] Processing " + task.getInputData().getOrDefault("interests", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("interests", List.of("technology"));
        r.getOutputData().put("demographics", Map.of());
        r.getOutputData().put("region", "US-West");
        r.getOutputData().put("accountAge", 450);
        return r;
    }
}
