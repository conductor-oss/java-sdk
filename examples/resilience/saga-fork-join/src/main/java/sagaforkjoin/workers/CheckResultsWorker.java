package sagaforkjoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sfj_check_results -- verifies all booking IDs are present.
 *
 * Input: { hotelBookingId, flightBookingId, carBookingId }
 * Output: { allValid: true/false, hotelBookingId, flightBookingId, carBookingId }
 *
 * If any booking ID is null or empty, allValid is false and the task fails.
 */
public class CheckResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sfj_check_results";
    }

    @Override
    public TaskResult execute(Task task) {
        String hotelBookingId = getStringInput(task, "hotelBookingId");
        String flightBookingId = getStringInput(task, "flightBookingId");
        String carBookingId = getStringInput(task, "carBookingId");

        boolean allValid = isPresent(hotelBookingId)
                && isPresent(flightBookingId)
                && isPresent(carBookingId);

        System.out.println("  [sfj_check_results] hotel=" + hotelBookingId
                + " flight=" + flightBookingId
                + " car=" + carBookingId
                + " allValid=" + allValid);

        TaskResult result = new TaskResult(task);
        result.getOutputData().put("hotelBookingId", hotelBookingId);
        result.getOutputData().put("flightBookingId", flightBookingId);
        result.getOutputData().put("carBookingId", carBookingId);
        result.getOutputData().put("allValid", allValid);

        if (allValid) {
            result.setStatus(TaskResult.Status.COMPLETED);
        } else {
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "One or more bookings missing");
        }

        return result;
    }

    private String getStringInput(Task task, String key) {
        Object value = task.getInputData().get(key);
        return value != null ? value.toString() : null;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isEmpty();
    }
}
