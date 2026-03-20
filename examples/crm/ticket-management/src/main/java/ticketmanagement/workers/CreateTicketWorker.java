package ticketmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.Random;

public class CreateTicketWorker implements Worker {
    @Override public String getTaskDefName() { return "tkt_create"; }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = "TKT-" + (new Random().nextInt(90000) + 10000);
        System.out.println("  [create] Ticket created -> " + ticketId + ": \"" + task.getInputData().get("subject") + "\"");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ticketId", ticketId);
        result.getOutputData().put("createdAt", Instant.now().toString());
        return result;
    }
}
