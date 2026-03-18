package secretsmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Schedules secret rotation based on policy.
 */
public class ScheduleRotationWorker implements Worker {
    @Override public String getTaskDefName() { return "sec_schedule_rotation"; }

    @Override public TaskResult execute(Task task) {
        String secretId = (String) task.getInputData().get("secretId");
        Object rotationDaysObj = task.getInputData().get("rotationDays");
        if (secretId == null) secretId = "UNKNOWN";

        int rotationDays = 90; // default 90-day rotation
        if (rotationDaysObj instanceof Number) rotationDays = ((Number) rotationDaysObj).intValue();

        Instant nextRotation = Instant.now().plus(rotationDays, ChronoUnit.DAYS);

        System.out.println("  [rotation] " + secretId + " next rotation in " + rotationDays + " days");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("scheduled", true);
        result.getOutputData().put("rotationDays", rotationDays);
        result.getOutputData().put("nextRotationAt", nextRotation.toString());
        return result;
    }
}
