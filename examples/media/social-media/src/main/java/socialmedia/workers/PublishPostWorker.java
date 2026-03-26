package socialmedia.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PublishPostWorker implements Worker {
    @Override public String getTaskDefName() { return "soc_publish_post"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [publish] Processing " + task.getInputData().getOrDefault("postId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("postId", "POST-515-TW-001");
        r.getOutputData().put("postUrl", "https://twitter.example.com/status/515001");
        r.getOutputData().put("publishedAt", "2026-03-08T14:00:00Z");
        return r;
    }
}
