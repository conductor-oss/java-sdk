package appointmentscheduling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Books an appointment slot for a patient.
 * Input: patientId, providerId, slot, visitType
 * Output: appointmentId, booked, bookedAt
 */
public class BookWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "apt_book";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String visitType = (String) task.getInputData().get("visitType");
        Map<String, Object> slot = (Map<String, Object>) task.getInputData().get("slot");
        if (slot == null) slot = Map.of();
        if (visitType == null) visitType = "general";

        System.out.println("  [book] Booking " + visitType + " on " + slot.get("date") + " at " + slot.get("time"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("appointmentId", "APT-20240315-001");
        result.getOutputData().put("booked", true);
        result.getOutputData().put("bookedAt", "2024-03-15T10:00:00Z");
        return result;
    }
}
