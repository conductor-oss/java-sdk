package sagapattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Compensation worker: cancels a previously reserved hotel by removing
 * the reservation entry from the BookingStore.
 *
 * Output:
 * - cancelled (boolean): true
 * - removedFromStore (boolean): whether the reservation was found and removed
 */
public class CancelHotelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "saga_cancel_hotel";
    }

    @Override
    public TaskResult execute(Task task) {
        String tripId = (String) task.getInputData().get("tripId");
        String reservationId = (String) task.getInputData().get("reservationId");
        if (reservationId == null) {
            reservationId = "HTL-" + tripId;
        }

        // Remove from booking store
        String removed = BookingStore.HOTEL_RESERVATIONS.remove(reservationId);

        System.out.println("  [cancel_hotel] Cancelling hotel " + reservationId
                + " for trip " + tripId + " (compensation) -- removed=" + (removed != null));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cancelled", true);
        result.getOutputData().put("removedFromStore", removed != null);
        return result;
    }
}
