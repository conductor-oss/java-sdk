package waitforevent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Prepares a request before the workflow pauses at the WAIT task.
 *
 * Takes requestId and requester from the workflow input.
 * Returns { prepared: true, requestId, requester } to confirm readiness.
 */
public class PrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "we_prepare";
    }

    @Override
    public TaskResult execute(Task task) {
        String requestId = (String) task.getInputData().get("requestId");
        String requester = (String) task.getInputData().get("requester");

        System.out.println("  [prepare] Preparing request " + requestId
                + " from " + requester);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("prepared", true);
        result.getOutputData().put("requestId", requestId);
        result.getOutputData().put("requester", requester);
        return result;
    }
}
