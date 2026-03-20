package contentsyndication.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class FormatPerPlatformWorker implements Worker {
    @Override public String getTaskDefName() { return "syn_format_per_platform"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [format] Processing " + task.getInputData().getOrDefault("formattedVersions", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("formattedVersions", List.of(
            Map.of("platform", "medium", "format", "markdown", "charCount", 1200),
            Map.of("platform", "wordpress", "format", "html", "charCount", 1200)
        ));
        return r;
    }
}
