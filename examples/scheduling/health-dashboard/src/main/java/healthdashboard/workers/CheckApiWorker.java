package healthdashboard.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckApiWorker implements Worker {
    @Override public String getTaskDefName() { return "hd_check_api"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [api] Checking api health in " + task.getInputData().get("environment"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("status", "healthy");
        r.getOutputData().put("responseTimeMs", 45);
        return r;
    }
}
