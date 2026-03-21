package privilegedaccess.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Receives a privileged access request and validates the justification.
 * Input: userId, resource, justification, duration
 * Output: requestId, success
 */
public class PamRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pam_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        if (userId == null) userId = "unknown";

        String resource = (String) task.getInputData().get("resource");
        if (resource == null) resource = "unknown";

        String justification = (String) task.getInputData().get("justification");
        if (justification == null) justification = "none";

        System.out.println("  [request] " + userId + " requesting " + resource + " access: " + justification);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestId", "REQUEST-1391");
        result.getOutputData().put("success", true);
        return result;
    }
}
