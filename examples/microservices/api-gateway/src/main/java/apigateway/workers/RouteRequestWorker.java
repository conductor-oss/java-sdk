package apigateway.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Routes the API request to the appropriate backend service.
 * Input: endpoint, method, payload, clientId
 * Output: rawResponse, statusCode, backendLatency
 */
public class RouteRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ag_route_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String method = (String) task.getInputData().get("method");
        String endpoint = (String) task.getInputData().get("endpoint");
        if (method == null) method = "GET";
        if (endpoint == null) endpoint = "/unknown";

        System.out.println("  [ag_route_request] Routing " + method + " " + endpoint + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rawResponse", Map.of(
                "users", List.of(
                        Map.of("id", 1, "name", "Alice", "email", "alice@example.com"),
                        Map.of("id", 2, "name", "Bob", "email", "bob@example.com")
                )
        ));
        result.getOutputData().put("statusCode", 200);
        result.getOutputData().put("backendLatency", 45);
        return result;
    }
}
