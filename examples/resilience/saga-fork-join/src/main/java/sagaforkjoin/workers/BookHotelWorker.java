package sagaforkjoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sfj_book_hotel -- books a hotel and returns a deterministic booking ID.
 *
 * Input: { tripId: string }
 * Output: { bookingId: "HTL-001", type: "hotel", tripId: string }
 */
public class BookHotelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sfj_book_hotel";
    }

    @Override
    public TaskResult execute(Task task) {
        String tripId = task.getInputData().get("tripId") != null
                ? task.getInputData().get("tripId").toString()
                : "unknown";

        System.out.println("  [sfj_book_hotel] Booking hotel for trip " + tripId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bookingId", "HTL-001");
        result.getOutputData().put("type", "hotel");
        result.getOutputData().put("tripId", tripId);
        return result;
    }
}
