package ticketmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class ResolveTicketWorker implements Worker {
    @Override public String getTaskDefName() { return "tkt_resolve"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [resolve] " + task.getInputData().get("ticketId") + " resolved by " + task.getInputData().get("assignee"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("resolution", "Issue fixed - cache invalidation corrected");
        result.getOutputData().put("resolvedAt", Instant.now().toString());
        return result;
    }
}
