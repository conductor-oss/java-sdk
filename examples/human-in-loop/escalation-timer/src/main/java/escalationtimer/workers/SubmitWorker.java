package escalationtimer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for et_submit -- submits a request for approval.
 *
 * Reads the "requestId" field from input and returns { submitted: true }.
 */
public class SubmitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "et_submit";
    }

    @Override
    public TaskResult execute(Task task) {
        String requestId = "unknown";
        Object requestIdInput = task.getInputData().get("requestId");
        if (requestIdInput instanceof String) {
            requestId = (String) requestIdInput;
        }

        System.out.println("  [et_submit] Request " + requestId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("submitted", true);

        return result;
    }
}
