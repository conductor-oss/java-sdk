package sagaforkjoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sfj_book_flight -- books a flight and returns a deterministic booking ID.
 *
 * Input: { tripId: string }
 * Output: { bookingId: "FLT-001", type: "flight", tripId: string }
 */
public class BookFlightWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sfj_book_flight";
    }

    @Override
    public TaskResult execute(Task task) {
        String tripId = task.getInputData().get("tripId") != null
                ? task.getInputData().get("tripId").toString()
                : "unknown";

        System.out.println("  [sfj_book_flight] Booking flight for trip " + tripId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bookingId", "FLT-001");
        result.getOutputData().put("type", "flight");
        result.getOutputData().put("tripId", tripId);
        return result;
    }
}
