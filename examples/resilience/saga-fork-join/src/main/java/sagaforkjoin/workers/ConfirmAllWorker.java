package sagaforkjoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sfj_confirm_all -- confirms the entire trip if all bookings are valid.
 *
 * Input: { tripId, hotelBookingId, flightBookingId, carBookingId, allValid }
 * Output: { confirmed: true/false, tripId, message }
 *
 * Only confirms when allValid is true.
 */
public class ConfirmAllWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sfj_confirm_all";
    }

    @Override
    public TaskResult execute(Task task) {
        String tripId = task.getInputData().get("tripId") != null
                ? task.getInputData().get("tripId").toString()
                : "unknown";

        Object allValidInput = task.getInputData().get("allValid");
        boolean allValid = Boolean.TRUE.equals(allValidInput)
                || "true".equalsIgnoreCase(String.valueOf(allValidInput));

        System.out.println("  [sfj_confirm_all] tripId=" + tripId + " allValid=" + allValid);

        TaskResult result = new TaskResult(task);
        result.getOutputData().put("tripId", tripId);

        if (allValid) {
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("confirmed", true);
            result.getOutputData().put("message", "Trip " + tripId + " confirmed with all bookings");
        } else {
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("confirmed", false);
            result.getOutputData().put("message", "Trip " + tripId + " rolled back -- not all bookings valid");
        }

        return result;
    }
}
