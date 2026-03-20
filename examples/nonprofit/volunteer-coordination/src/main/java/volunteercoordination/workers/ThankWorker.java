package volunteercoordination.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ThankWorker implements Worker {
    @Override public String getTaskDefName() { return "vol_thank"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [thank] Thanking " + task.getInputData().get("volunteerName") + " for " + task.getInputData().get("hours") + " hours");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("volunteer", Map.of("name", task.getInputData().getOrDefault("volunteerName","Maria Garcia"), "hours", task.getInputData().getOrDefault("hours",4), "status", "APPRECIATED")); return r;
    }
}
