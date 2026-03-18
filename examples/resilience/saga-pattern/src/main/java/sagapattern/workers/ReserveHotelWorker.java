package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Reserves a hotel room for the trip. Creates a real entry in the
 * BookingStore that can be cancelled by CancelHotelWorker.
 *
 * Output:
 * - reservationId (String): "HTL-" + tripId
 * - reservedAt (String): ISO-8601 timestamp
 */
public class ReserveHotelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_reserve_hotel";
    }

    @Override
    public TaskResult execute(Task task) {
        String tripId = (String) task.getInputData().get("tripId");
        String reservationId = "HTL-" + tripId;

        // Create real booking entry
        BookingStore.HOTEL_RESERVATIONS.put(reservationId, Instant.now().toString());

        System.out.println("  [reserve_hotel] Reserving hotel for trip " + tripId
                + " -- reservationId=" + reservationId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reservationId", reservationId);
        result.getOutputData().put("reservedAt", Instant.now().toString());
        return result;
    }
}
