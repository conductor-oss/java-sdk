package appointmentscheduling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Schedules a reminder notification for the appointment.
 * Input: patientId, appointmentId, slot
 * Output: reminderSet, reminderTime
 */
public class RemindWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "apt_remind";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> slot = (Map<String, Object>) task.getInputData().get("slot");
        if (slot == null) slot = Map.of();

        System.out.println("  [remind] Reminder scheduled for 24h before " + slot.get("date") + " " + slot.get("time"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reminderSet", true);
        result.getOutputData().put("reminderTime", "24h before");
        return result;
    }
}
