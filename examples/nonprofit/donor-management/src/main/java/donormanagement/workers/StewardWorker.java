package donormanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class StewardWorker implements Worker {
    @Override public String getTaskDefName() { return "dnr_steward"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [steward] Stewarding donor " + task.getInputData().get("donorId") + ", first gift: $" + task.getInputData().get("firstGift"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("engagement", Map.of("touchpoints", 3, "lastContact", "2026-03-08", "sentiment", "positive")); return r;
    }
}
