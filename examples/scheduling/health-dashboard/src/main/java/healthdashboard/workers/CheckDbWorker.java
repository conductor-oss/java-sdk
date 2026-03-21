package healthdashboard.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckDbWorker implements Worker {
    @Override public String getTaskDefName() { return "hd_check_db"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [database] Checking database health in " + task.getInputData().get("environment"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("status", "healthy");
        r.getOutputData().put("responseTimeMs", 12);
        return r;
    }
}
