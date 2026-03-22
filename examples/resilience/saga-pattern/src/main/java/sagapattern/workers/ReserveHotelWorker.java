package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Reserves a hotel room for the trip. Creates a real entry in the
 * BookingStore that can be cancelled by CancelHotelWorker.
 *
 * Input:
 *   - tripId (String, required): trip identifier
 *   - shouldFail (boolean, optional): if true, simulates hotel reservation failure
 *
 * Output:
 *   - reservationId (String): "HTL-" + tripId
 *   - reservedAt (String): ISO-8601 timestamp
 */
public class ReserveHotelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_reserve_hotel";
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

        Object shouldFailObj = task.getInputData().get("shouldFail");
        boolean shouldFail = Boolean.TRUE.equals(shouldFailObj)
                || "true".equals(String.valueOf(shouldFailObj));

        if (shouldFail) {
            System.out.println("  [reserve_hotel] Hotel reservation FAILED for trip " + tripId);
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Hotel reservation failed for trip " + tripId);
            result.getOutputData().put("status", "failed");
            return result;
        }

        String reservationId = "HTL-" + tripId;

        // Create real booking entry
        BookingStore.HOTEL_RESERVATIONS.put(reservationId, Instant.now().toString());
        BookingStore.recordAction("RESERVE_HOTEL", reservationId);

        System.out.println("  [reserve_hotel] Reserving hotel for trip " + tripId
                + " -- reservationId=" + reservationId);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reservationId", reservationId);
        result.getOutputData().put("reservedAt", Instant.now().toString());
        return result;
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }
}
