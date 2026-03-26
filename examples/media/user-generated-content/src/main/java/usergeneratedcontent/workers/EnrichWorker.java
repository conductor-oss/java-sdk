package usergeneratedcontent.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class EnrichWorker implements Worker {
    @Override public String getTaskDefName() { return "ugc_enrich"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [enrich] Processing " + task.getInputData().getOrDefault("metadata", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("metadata", Map.of());
        r.getOutputData().put("autoTags", List.of("review"));
        r.getOutputData().put("sentiment", "positive");
        r.getOutputData().put("readability", "grade_8");
        r.getOutputData().put("language", "en");
        return r;
    }
}
