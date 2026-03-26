package socialmedia.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class CreateContentWorker implements Worker {
    @Override public String getTaskDefName() { return "soc_create_content"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [create] Processing " + task.getInputData().getOrDefault("contentId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("contentId", "SOC-CNT-515-001");
        r.getOutputData().put("formattedMessage", task.getInputData().get("message"));
        r.getOutputData().put("hashtags", List.of("#workflow"));
        r.getOutputData().put("optimalTime", "2026-03-08T14:00:00Z");
        return r;
    }
}
