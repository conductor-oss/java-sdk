package waitsdk.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for wsdk_finalize — closes a ticket after the WAIT task is resolved.
 *
 * Input:  { ticketId: string }
 * Output: { result: "closed-{ticketId}" }
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wsdk_finalize";
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
        result.getOutputData().put("result", "closed-" + ticketId);
        return result;
    }
}
