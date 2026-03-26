package releasenotesai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class PublishWorker implements Worker {
    @Override public String getTaskDefName() { return "rna_publish"; }
    @Override public TaskResult execute(Task task) {
        String toTag = (String) task.getInputData().getOrDefault("toTag", "unknown");
        System.out.println("  [publish] Published release notes for " + toTag);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("published", true);
        result.getOutputData().put("version", toTag);
        return result;
    }
}
