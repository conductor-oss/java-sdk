package reservationsystem.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BookWorker implements Worker {
    @Override public String getTaskDefName() { return "rsv_book"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [book] Booking for " + task.getInputData().get("guestName"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("reservationId", "RSV-736");
        result.addOutputData("booked", true);
        return result;
    }
}
