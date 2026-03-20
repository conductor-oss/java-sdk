package eventfundraising.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class PromoteWorker implements Worker {
    @Override public String getTaskDefName() { return "efr_promote"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [promote] Promoting " + task.getInputData().get("eventName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("registrations", 245); r.addOutputData("channels", List.of("email","social","partners")); return r;
    }
}
