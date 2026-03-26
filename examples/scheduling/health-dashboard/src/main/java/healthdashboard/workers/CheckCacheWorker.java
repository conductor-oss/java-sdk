package healthdashboard.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckCacheWorker implements Worker {
    @Override public String getTaskDefName() { return "hd_check_cache"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [cache] Checking cache health in " + task.getInputData().get("environment"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("status", "healthy");
        r.getOutputData().put("responseTimeMs", 5);
        return r;
    }
}
