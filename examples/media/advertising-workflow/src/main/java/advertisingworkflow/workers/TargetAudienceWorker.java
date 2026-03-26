package advertisingworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class TargetAudienceWorker implements Worker {
    @Override public String getTaskDefName() { return "adv_target_audience"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [target] Processing " + task.getInputData().getOrDefault("audienceSize", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("audienceSize", 2500000);
        r.getOutputData().put("segments", List.of("tech_professionals"));
        r.getOutputData().put("demographics", Map.of());
        r.getOutputData().put("interests", List.of("technology"));
        return r;
    }
}
