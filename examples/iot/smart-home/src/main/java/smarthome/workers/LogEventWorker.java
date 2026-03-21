package smarthome.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class LogEventWorker implements Worker {
    @Override public String getTaskDefName() { return "smh_log_event"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [log] Processing " + task.getInputData().getOrDefault("logged", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("logged", true);
        r.getOutputData().put("logId", "LOG-536-001");
        r.getOutputData().put("loggedAt", "2026-03-08T19:30:01Z");
        return r;
    }
}
