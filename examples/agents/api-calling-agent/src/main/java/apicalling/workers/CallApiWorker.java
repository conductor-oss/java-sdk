package apicalling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Performs a real HTTP GET request to the specified API endpoint using {@link HttpClient}.
 *
 * <p>Takes an endpoint URL, HTTP method, optional query params, and an auth token from the task
 * input. Executes the request and returns the HTTP status code, measured response time in
 * milliseconds, and a preview of the response body (first 2000 characters).
 *
 * <p>Errors (connection failures, timeouts, non-2xx responses) are captured gracefully and
 * returned as part of the output rather than failing the task.
 */
public class CallApiWorker implements Worker {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String getTaskDefName() {
        return "ap_call_api";
    }

    @Override
    public TaskResult execute(Task task) {
        String endpoint = (String) task.getInputData().get("endpoint");
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "https://api.github.com/repos/conductor-oss/conductor";
        }

        String method = (String) task.getInputData().get("method");
        if (method == null || method.isBlank()) {
            method = "GET";
        }

        String authToken = (String) task.getInputData().get("authToken");

        System.out.println("  [ap_call_api] Calling " + method + " " + endpoint);

        TaskResult result = new TaskResult(task);

        long startTime = System.currentTimeMillis();
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("User-Agent", "ConductorExample/1.0");

            if (authToken != null && !authToken.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + authToken);
            }

            // Only GET is supported for now
            reqBuilder.GET();

            HttpRequest request = reqBuilder.build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            long elapsed = System.currentTimeMillis() - startTime;
            String body = response.body();

            Map<String, Object> responseMap = new LinkedHashMap<>();
            responseMap.put("bodyPreview", body.length() > 2000 ? body.substring(0, 2000) + "..." : body);
            responseMap.put("bodyLength", body.length());
            responseMap.put("contentType", response.headers().firstValue("content-type").orElse("unknown"));

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("response", responseMap);
            result.getOutputData().put("statusCode", response.statusCode());
            result.getOutputData().put("responseTimeMs", elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("response", errorResponse);
            result.getOutputData().put("statusCode", 0);
            result.getOutputData().put("responseTimeMs", elapsed);
        }

        return result;
    }
}
