package restaurantmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class ReservationsWorker implements Worker {
    @Override public String getTaskDefName() { return "rst_reservations"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [reserve] Reservation for " + task.getInputData().get("guestName") + ", party of " + task.getInputData().get("partySize"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("reservation", Map.of("id", "RES-201", "time", "7:00 PM", "confirmed", true));
        return result;
    }
}
