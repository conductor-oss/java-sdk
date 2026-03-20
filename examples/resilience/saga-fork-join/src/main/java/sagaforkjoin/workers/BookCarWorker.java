package sagaforkjoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sfj_book_car -- books a rental car and returns a deterministic booking ID.
 *
 * Input: { tripId: string }
 * Output: { bookingId: "CAR-001", type: "car", tripId: string }
 */
public class BookCarWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sfj_book_car";
    }

    @Override
    public TaskResult execute(Task task) {
        String tripId = task.getInputData().get("tripId") != null
                ? task.getInputData().get("tripId").toString()
                : "unknown";

        System.out.println("  [sfj_book_car] Booking car for trip " + tripId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bookingId", "CAR-001");
        result.getOutputData().put("type", "car");
        result.getOutputData().put("tripId", tripId);
        return result;
    }
}
