package apigateway.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Sends the final API response to the client.
 * Input: transformedResponse, statusCode
 * Output: statusCode, body, headers
 */
public class SendResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ag_send_response";
    }

    @Override
    public TaskResult execute(Task task) {
        Object transformedResponse = task.getInputData().get("transformedResponse");
        Object statusCodeObj = task.getInputData().get("statusCode");
        int statusCode = 200;
        if (statusCodeObj instanceof Number) {
            statusCode = ((Number) statusCodeObj).intValue();
        }

        System.out.println("  [ag_send_response] Sending response with status " + statusCode + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("statusCode", statusCode);
        result.getOutputData().put("body", transformedResponse);
        result.getOutputData().put("headers", Map.of(
                "Content-Type", "application/json",
                "X-Request-Id", "req-abc-123",
                "X-RateLimit-Remaining", "9999"
        ));
        return result;
    }
}
