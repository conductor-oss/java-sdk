package tenantscreening.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ApplyWorker implements Worker {
    @Override public String getTaskDefName() { return "tsc_apply"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [tsc_apply] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("applicationId", "TSC-684");
        result.getOutputData().put("score", 700);
        return result;
    }
}
