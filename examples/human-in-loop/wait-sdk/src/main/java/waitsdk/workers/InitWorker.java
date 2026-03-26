package waitsdk.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for wsdk_init — initializes a ticket with status "open".
 *
 * Input:  { ticketId: string }
 * Output: { ticketId: string, status: "open" }
 */
public class InitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wsdk_init";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = "";
        Object ticketIdInput = task.getInputData().get("ticketId");
        if (ticketIdInput != null) {
            ticketId = ticketIdInput.toString();
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ticketId", ticketId);
        result.getOutputData().put("status", "open");
        return result;
    }
}
