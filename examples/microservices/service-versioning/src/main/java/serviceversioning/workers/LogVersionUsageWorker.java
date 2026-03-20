package serviceversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class LogVersionUsageWorker implements Worker {
    @Override public String getTaskDefName() { return "sv_log_version_usage"; }
    @Override public TaskResult execute(Task task) {
        String resolved = (String) task.getInputData().getOrDefault("resolved", "unknown");
        System.out.println("  [log] Version usage: " + resolved);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("logged", true);
        return r;
    }
}
