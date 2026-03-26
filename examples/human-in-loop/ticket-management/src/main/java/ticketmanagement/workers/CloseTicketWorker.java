package ticketmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class CloseTicketWorker implements Worker {
    @Override public String getTaskDefName() { return "tkt_close"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [close] " + task.getInputData().get("ticketId") + " closed");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("closed", true);
        result.getOutputData().put("closedAt", Instant.now().toString());
        return result;
    }
}
