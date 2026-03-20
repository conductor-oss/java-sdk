package apigatewayrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Routes a request to the appropriate backend service.
 * Input: path, method, body, clientId
 * Output: statusCode, response
 */
public class RouteRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gw_route_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String method = (String) task.getInputData().get("method");
        String path = (String) task.getInputData().get("path");
        if (method == null) method = "GET";
        if (path == null) path = "/unknown";

        System.out.println("  [route] " + method + " " + path + " -> order-service");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("statusCode", 200);
        result.getOutputData().put("response", Map.of("orderId", "ORD-123", "status", "confirmed"));
        return result;
    }
}
