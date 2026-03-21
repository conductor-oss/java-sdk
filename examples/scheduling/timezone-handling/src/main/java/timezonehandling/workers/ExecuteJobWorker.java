package timezonehandling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExecuteJobWorker implements Worker {
    @Override public String getTaskDefName() { return "tz_execute_job"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [execute] Executing \"" + task.getInputData().get("jobName") + "\" at " + task.getInputData().get("scheduledUtc"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("executed", true);
        r.getOutputData().put("completedAt", "2026-03-08T17:00:05Z");
        r.getOutputData().put("durationMs", 5000);
        return r;
    }
}
