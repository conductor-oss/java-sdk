package grantmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ApplyWorker implements Worker {
    @Override public String getTaskDefName() { return "gmt_apply"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [apply] Application from " + task.getInputData().get("organization") + " for " + task.getInputData().get("program"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("application", Map.of("id", "APP-752", "status", "SUBMITTED", "date", "2026-03-08"));
        return r;
    }
}
