package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Books a flight for the trip. Creates a real entry in the
 * BookingStore that can be cancelled by CancelFlightWorker.
 *
 * Output:
 * - bookingId (String): "FLT-" + tripId
 * - bookedAt (String): ISO-8601 timestamp
 */
public class BookFlightWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_book_flight";
    }

    @Override
    public TaskResult execute(Task task) {
        String tripId = (String) task.getInputData().get("tripId");
        String bookingId = "FLT-" + tripId;

        // Create real booking entry
        BookingStore.FLIGHT_BOOKINGS.put(bookingId, Instant.now().toString());

        System.out.println("  [book_flight] Booking flight for trip " + tripId
                + " -- bookingId=" + bookingId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bookingId", bookingId);
        result.getOutputData().put("bookedAt", Instant.now().toString());
        return result;
    }
}
