package scheduledevent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks the schedule and determines the delay until execution.
 * Input: scheduledTime
 * Output: delayMs (0), ready (true)
 */
public class CheckScheduleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "se_check_schedule";
    }

    @Override
    public TaskResult execute(Task task) {
        String scheduledTime = (String) task.getInputData().get("scheduledTime");
        if (scheduledTime == null) {
            scheduledTime = "1970-01-01T00:00:00Z";
        }

        System.out.println("  [se_check_schedule] Checking schedule for: " + scheduledTime);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delayMs", 0);
        result.getOutputData().put("ready", true);
        return result;
    }
}
