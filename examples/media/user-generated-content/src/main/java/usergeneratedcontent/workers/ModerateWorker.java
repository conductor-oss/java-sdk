package usergeneratedcontent.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class ModerateWorker implements Worker {
    @Override public String getTaskDefName() { return "ugc_moderate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [moderate] Processing " + task.getInputData().getOrDefault("moderationScore", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("moderationScore", 0.95);
        r.getOutputData().put("flagged", false);
        r.getOutputData().put("categories", Map.of());
        r.getOutputData().put("spam", 0.01);
        r.getOutputData().put("adult", 0.0);
        return r;
    }
}
