package uptimemonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CheckEndpointWorker implements Worker {
    @Override public String getTaskDefName() { return "um_check_endpoint"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [check] Checking " + task.getInputData().get("endpoint"));
        r.getOutputData().put("httpStatus", 200);
        r.getOutputData().put("responseTimeMs", 85);
        r.getOutputData().put("isUp", true);
        return r;
    }
}
