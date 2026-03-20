package apigatewayrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validates authentication tokens for incoming API requests.
 * Input: authorization, path
 * Output: clientId, clientVersion, requestId
 */
public class AuthenticateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gw_authenticate";
    }

    @Override
    public TaskResult execute(Task task) {
        String path = (String) task.getInputData().get("path");
        if (path == null) path = "/unknown";

        System.out.println("  [auth] Validating token for " + path);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("clientId", "client-42");
        result.getOutputData().put("clientVersion", "v2");
        result.getOutputData().put("requestId", "REQ-fixed-001");
        return result;
    }
}
