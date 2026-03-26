package crontrigger.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Logs that a scheduled job was skipped.
 * Input: jobName, reason
 * Output: skipped (true), reason
 *
 * Deterministic: passes through the reason from input.
 */
public class LogSkipWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cn_log_skip";
    }

    @Override
    public TaskResult execute(Task task) {
        String jobName = (String) task.getInputData().get("jobName");
        if (jobName == null) {
            jobName = "unknown";
        }

        String reason = (String) task.getInputData().get("reason");
        if (reason == null) {
            reason = "no reason provided";
        }

        System.out.println("  [cn_log_skip] Job \"" + jobName + "\" skipped: " + reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("skipped", true);
        result.getOutputData().put("reason", reason);
        return result;
    }
}
