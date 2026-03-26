package documentationai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class PublishWorker implements Worker {
    @Override public String getTaskDefName() { return "doc_publish"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [publish] Published documentation");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("publishUrl", "https://docs.example.com/v2");
        result.getOutputData().put("published", true);
        return result;
    }
}
