package travelbooking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Confirms booking and sends confirmation.
 */
public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "tvb_confirm"; }

    @Override public TaskResult execute(Task task) {
        String bookingRef = (String) task.getInputData().get("bookingRef");
        Object bookedObj = task.getInputData().get("booked");
        boolean booked = Boolean.TRUE.equals(bookedObj);
        if (bookingRef == null) bookingRef = "NONE";

        System.out.println("  [confirm] " + (booked ? "Confirmed " + bookingRef : "Nothing to confirm"));

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("confirmed", booked);
        r.getOutputData().put("confirmationSent", booked);
        return r;
    }
}
