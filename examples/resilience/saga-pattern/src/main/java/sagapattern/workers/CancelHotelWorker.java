package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Compensation worker: cancels a previously reserved hotel by removing
 * the reservation entry from the BookingStore.
 *
 * Input:
 *   - tripId (String, required): trip identifier
 *   - reservationId (String, optional): explicit reservation ID; defaults to "HTL-" + tripId
 *
 * Output:
 *   - cancelled (boolean): true
 *   - removedFromStore (boolean): whether the reservation was found and removed
 */
public class CancelHotelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_cancel_hotel";
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

        String reservationId = getRequiredString(task, "reservationId");
        if (reservationId == null || reservationId.isBlank()) {
            reservationId = "HTL-" + tripId;
        }

        // Remove from booking store
        String removed = BookingStore.HOTEL_RESERVATIONS.remove(reservationId);
        BookingStore.recordAction("CANCEL_HOTEL", reservationId);

        System.out.println("  [cancel_hotel] Cancelling hotel " + reservationId
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
