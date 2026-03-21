package appointmentscheduling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Checks provider availability and returns an available slot.
 * Input: providerId, preferredDate, visitType
 * Output: availableSlot, alternateSlots
 */
public class CheckAvailabilityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "apt_check_availability";
    }

    @Override
    public TaskResult execute(Task task) {
        String providerId = (String) task.getInputData().get("providerId");
        String preferredDate = (String) task.getInputData().get("preferredDate");
        if (providerId == null) providerId = "UNKNOWN";
        if (preferredDate == null) preferredDate = "2024-01-01";

        System.out.println("  [availability] Checking slots for provider " + providerId + " on " + preferredDate);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("availableSlot", Map.of(
                "date", preferredDate, "time", "10:30 AM", "duration", 30));
        result.getOutputData().put("alternateSlots", List.of(
                Map.of("date", preferredDate, "time", "2:00 PM", "duration", 30),
                Map.of("date", preferredDate, "time", "3:30 PM", "duration", 30)));
        return result;
    }
}
