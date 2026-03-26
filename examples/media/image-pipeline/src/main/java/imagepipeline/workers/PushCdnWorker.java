package imagepipeline.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class PushCdnWorker implements Worker {
    @Override public String getTaskDefName() { return "imp_push_cdn"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [cdn] Processing " + task.getInputData().getOrDefault("cdnUrls", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("cdnUrls", List.of());
        r.getOutputData().put("cdnBaseUrl", "https://cdn.example.com/img/513/200x150.jpg");
        r.getOutputData().put("cacheInvalidated", true);
        r.getOutputData().put("ttlSeconds", 86400);
        return r;
    }
}
