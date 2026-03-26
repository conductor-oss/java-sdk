package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Compensation worker: cancels a previously booked flight by removing
 * the booking entry from the BookingStore.
 *
 * Input:
 *   - tripId (String, required): trip identifier
 *   - bookingId (String, optional): explicit booking ID; defaults to "FLT-" + tripId
 *
 * Output:
 *   - cancelled (boolean): true
 *   - removedFromStore (boolean): whether the booking was found and removed
 */
public class CancelFlightWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_cancel_flight";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        String tripId = getRequiredString(task, "tripId");
        if (tripId == null || tripId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: tripId");
            return result;
        }

        String bookingId = getRequiredString(task, "bookingId");
        if (bookingId == null || bookingId.isBlank()) {
            bookingId = "FLT-" + tripId;
        }

        // Remove from booking store
        String removed = BookingStore.FLIGHT_BOOKINGS.remove(bookingId);
        BookingStore.recordAction("CANCEL_FLIGHT", bookingId);

        System.out.println("  [cancel_flight] Cancelling flight " + bookingId
                + " for trip " + tripId + " (compensation) -- removed=" + (removed != null));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cancelled", true);
        result.getOutputData().put("removedFromStore", removed != null);
        return result;
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }
}
