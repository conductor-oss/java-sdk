package apicalling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Plans an API call based on a user request and an API catalog.
 * Selects the best API, determines the endpoint, method, params, auth type,
 * and expected response schema.
 */
public class PlanApiCallWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ap_plan_api_call";
    }

    @Override
    public TaskResult execute(Task task) {
        String userRequest = (String) task.getInputData().get("userRequest");
        if (userRequest == null || userRequest.isBlank()) {
            userRequest = "general API request";
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> apiCatalog =
                (List<Map<String, Object>>) task.getInputData().get("apiCatalog");

        System.out.println("  [ap_plan_api_call] Planning API call for request: " + userRequest);

        Map<String, Object> params = Map.of(
                "owner", "conductor-oss",
                "repo", "conductor"
        );

        Map<String, Object> responseSchema = Map.of(
                "type", "object",
                "fields", List.of("full_name", "description", "stargazers_count",
                        "forks_count", "language", "license", "open_issues_count",
                        "default_branch")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("selectedApi", "github");
        result.getOutputData().put("endpoint", "https://api.github.com/repos/conductor-oss/conductor");
        result.getOutputData().put("method", "GET");
        result.getOutputData().put("params", params);
        result.getOutputData().put("authType", "bearer_token");
        result.getOutputData().put("responseSchema", responseSchema);
        return result;
    }
}
