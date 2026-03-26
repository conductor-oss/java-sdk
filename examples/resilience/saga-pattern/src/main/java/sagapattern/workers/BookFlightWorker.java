package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Books a flight for the trip. Creates a real entry in the
 * BookingStore that can be cancelled by CancelFlightWorker.
 *
 * Input:
 *   - tripId (String, required): trip identifier
 *
 * Output:
 *   - bookingId (String): "FLT-" + tripId
 *   - bookedAt (String): ISO-8601 timestamp
 */
public class BookFlightWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_book_flight";
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

        String bookingId = "FLT-" + tripId;

        // Create real booking entry
        BookingStore.FLIGHT_BOOKINGS.put(bookingId, Instant.now().toString());
        BookingStore.recordAction("BOOK_FLIGHT", bookingId);

        System.out.println("  [book_flight] Booking flight for trip " + tripId
                + " -- bookingId=" + bookingId);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bookingId", bookingId);
        result.getOutputData().put("bookedAt", Instant.now().toString());
        return result;
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }
}
