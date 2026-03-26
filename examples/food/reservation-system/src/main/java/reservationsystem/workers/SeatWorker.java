package reservationsystem.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class SeatWorker implements Worker {
    @Override public String getTaskDefName() { return "rsv_seat"; }
    @Override public TaskResult execute(Task task) {
        String reservationId = (String) task.getInputData().get("reservationId");
        System.out.println("  [seat] Seating party of " + task.getInputData().get("partySize") + " for reservation " + reservationId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("seated", Map.of("reservationId", reservationId != null ? reservationId : "RSV-736", "table", "T-8", "status", "SEATED"));
        return result;
    }
}
