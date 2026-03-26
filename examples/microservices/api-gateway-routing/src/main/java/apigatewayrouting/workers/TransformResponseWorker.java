package apigatewayrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Transforms a service response for the client version.
 * Input: serviceResponse, clientVersion
 * Output: transformedBody
 */
public class TransformResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gw_transform_response";
    }

    @Override
    public TaskResult execute(Task task) {
        Object serviceResponse = task.getInputData().get("serviceResponse");
        String clientVersion = (String) task.getInputData().get("clientVersion");
        if (clientVersion == null) clientVersion = "v1";

        System.out.println("  [transform] Formatting response for " + clientVersion);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformedBody", serviceResponse);
        return result;
    }
}
