package crontrigger.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks whether a cron schedule matches the current time window.
 * Input: cronExpression, jobName
 * Output: shouldRun ("yes"/"no"), scheduledTime, cronExpression, skipReason (when no)
 *
 * Deterministic: always returns shouldRun="yes" with a fixed scheduledTime.
 */
public class CheckScheduleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cn_check_schedule";
    }

    @Override
    public TaskResult execute(Task task) {
        String cronExpression = (String) task.getInputData().get("cronExpression");
        if (cronExpression == null) {
            cronExpression = "unknown";
        }

        String jobName = (String) task.getInputData().get("jobName");
        if (jobName == null) {
            jobName = "unknown";
        }

        System.out.println("  [cn_check_schedule] Checking cron \"" + cronExpression
                + "\" for job \"" + jobName + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("shouldRun", "yes");
        result.getOutputData().put("scheduledTime", "2026-01-15T10:00:00Z");
        result.getOutputData().put("cronExpression", cronExpression);
        return result;
    }
}
