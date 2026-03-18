package apigateway.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Transforms the raw backend response into a client-friendly format.
 * Input: rawResponse, clientId
 * Output: transformed (data + meta)
 */
public class TransformResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ag_transform_response";
    }

    @Override
    public TaskResult execute(Task task) {
        Object rawResponse = task.getInputData().get("rawResponse");
        String clientId = (String) task.getInputData().get("clientId");
        if (clientId == null) clientId = "unknown";

        System.out.println("  [ag_transform_response] Transforming response for client " + clientId + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformed", Map.of(
                "data", rawResponse != null ? rawResponse : Map.of(),
                "meta", Map.of("count", 2, "page", 1, "totalPages", 1)
        ));
        return result;
    }
}
