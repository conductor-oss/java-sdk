package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Compensation worker: cancels a previously booked flight by removing
 * the booking entry from the BookingStore.
 *
 * Output:
 * - cancelled (boolean): true
 * - removedFromStore (boolean): whether the booking was found and removed
 */
public class CancelFlightWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_cancel_flight";
    }

    @Override
    public TaskResult execute(Task task) {
        String tripId = (String) task.getInputData().get("tripId");
        String bookingId = (String) task.getInputData().get("bookingId");
        if (bookingId == null) {
            bookingId = "FLT-" + tripId;
        }

        // Remove from booking store
        String removed = BookingStore.FLIGHT_BOOKINGS.remove(bookingId);

        System.out.println("  [cancel_flight] Cancelling flight " + bookingId
                + " for trip " + tripId + " (compensation) -- removed=" + (removed != null));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cancelled", true);
        result.getOutputData().put("removedFromStore", removed != null);
        return result;
    }
}
