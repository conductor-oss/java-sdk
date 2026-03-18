package travelbooking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Books the selected travel option. Real booking confirmation.
 */
public class BookWorker implements Worker {
    @Override public String getTaskDefName() { return "tvb_book"; }

    @Override public TaskResult execute(Task task) {
        Object withinBudgetObj = task.getInputData().get("withinBudget");
        boolean withinBudget = Boolean.TRUE.equals(withinBudgetObj);

        String bookingRef = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        boolean booked = withinBudget;

        System.out.println("  [book] " + (booked ? "Booked: " + bookingRef : "Booking failed - over budget"));

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("bookingRef", booked ? bookingRef : "NONE");
        r.getOutputData().put("booked", booked);
        r.getOutputData().put("bookedAt", booked ? Instant.now().toString() : null);
        return r;
    }
}
