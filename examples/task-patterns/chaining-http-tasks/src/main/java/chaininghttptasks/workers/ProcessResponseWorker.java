package chaininghttptasks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that processes the HTTP response from the API call.
 * Receives the status code and body from the preceding HTTP system task,
 * extracts useful information, and passes it forward.
 */
public class ProcessResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "http_process_response";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object statusCodeObj = task.getInputData().get("statusCode");
        int statusCode = 0;
        if (statusCodeObj instanceof Number) {
            statusCode = ((Number) statusCodeObj).intValue();
        }

        Object responseBody = task.getInputData().get("responseBody");
        int resultCount = 0;

        if (responseBody instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) responseBody;
            Object results = body.get("results");
            if (results instanceof List) {
                resultCount = ((List<?>) results).size();
            }
        }

        System.out.println("  [process] HTTP " + statusCode);
        System.out.println("  [process] Found " + resultCount + " workflows");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "Search returned " + resultCount + " results");
        result.getOutputData().put("httpStatus", statusCode);
        return result;
    }
}
