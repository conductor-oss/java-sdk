package insuranceunderwriting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
public class AcceptWorker implements Worker {
    @Override public String getTaskDefName() { return "uw_accept"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [accept] Application " + task.getInputData().get("applicationId") + " ACCEPTED — premium: $" + task.getInputData().get("premium") + "/mo");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("accepted", true); r.getOutputData().put("acceptedAt", Instant.now().toString());
        return r;
    }
}
