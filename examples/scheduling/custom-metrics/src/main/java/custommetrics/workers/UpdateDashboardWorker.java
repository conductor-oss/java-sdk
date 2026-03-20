package custommetrics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class UpdateDashboardWorker implements Worker {
    @Override public String getTaskDefName() { return "cus_update_dashboard"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [dashboard] Updating dashboard");
        r.getOutputData().put("dashboardUpdated", true);
        return r;
    }
}
