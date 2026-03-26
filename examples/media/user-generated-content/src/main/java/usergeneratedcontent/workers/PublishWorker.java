package usergeneratedcontent.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PublishWorker implements Worker {
    @Override public String getTaskDefName() { return "ugc_publish"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [publish] Processing " + task.getInputData().getOrDefault("published", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("published", "approved");
        r.getOutputData().put("publishUrl", "https://community.example.com/posts/527-001");
        r.getOutputData().put("publishedAt", "2026-03-08T09:02:00Z");
        return r;
    }
}
