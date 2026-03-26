package usersurvey.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class DistributeSurveyWorker implements Worker {
    @Override public String getTaskDefName() { return "usv_distribute"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [distribute] Survey sent to " + task.getInputData().get("audience") + " audience");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("sentTo", 2500);
        r.getOutputData().put("channels", List.of("email", "in-app"));
        return r;
    }
}
