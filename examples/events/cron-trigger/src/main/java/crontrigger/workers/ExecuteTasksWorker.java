package crontrigger.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Executes the scheduled tasks for a cron job.
 * Input: jobName, scheduledTime
 * Output: result ("success"), executedAt, duration
 *
 * Deterministic: always returns consistent executedAt and duration.
 */
public class ExecuteTasksWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cn_execute_tasks";
    }

    @Override
    public TaskResult execute(Task task) {
        String jobName = (String) task.getInputData().get("jobName");
        if (jobName == null) {
            jobName = "unknown";
        }

        String scheduledTime = (String) task.getInputData().get("scheduledTime");
        if (scheduledTime == null) {
            scheduledTime = "unknown";
        }

        System.out.println("  [cn_execute_tasks] Running job \"" + jobName
                + "\" at " + scheduledTime);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "success");
        result.getOutputData().put("executedAt", "2026-01-15T10:00:01Z");
        result.getOutputData().put("duration", 1250);
        return result;
    }
}
