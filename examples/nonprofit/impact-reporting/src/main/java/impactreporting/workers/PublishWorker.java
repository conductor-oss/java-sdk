package impactreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class PublishWorker implements Worker {
    @Override public String getTaskDefName() { return "ipr_publish"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [publish] Impact report published");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("impact", Map.of("published", true, "url", "https://example.org/impact-2025", "downloadable", true, "status", "PUBLISHED")); return r;
    }
}
