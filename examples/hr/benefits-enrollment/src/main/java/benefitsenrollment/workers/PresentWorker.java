package benefitsenrollment.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class PresentWorker implements Worker {
    @Override public String getTaskDefName() { return "ben_present"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [present] Presenting benefit options to " + task.getInputData().get("employeeId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("options", Map.of("medical", List.of("PPO","HMO","HDHP"), "dental", List.of("basic","premium"), "vision", List.of("standard")));
        return r;
    }
}
