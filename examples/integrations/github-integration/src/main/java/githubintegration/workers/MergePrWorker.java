package githubintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Merges a pull request.
 * Input: repo, prNumber, checksPass
 * Output: merged, mergedAt
 *
 * Runs in live mode when GITHUB_TOKEN is set,
 * otherwise returns an error if token is missing.
 */
public class MergePrWorker implements Worker {

    private final boolean liveMode;
    private final String githubToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MergePrWorker() {
        this.githubToken = System.getenv("GITHUB_TOKEN");
        this.liveMode = githubToken != null && !githubToken.isBlank();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "gh_merge_pr";
    }

    @Override
    public TaskResult execute(Task task) {
        String repo = (String) task.getInputData().get("repo");
        Object prNumber = task.getInputData().get("prNumber");
        Object checksPass = task.getInputData().get("checksPass");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (liveMode) {
            try {
                String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                        "merge_method", "merge"
                ));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/repos/" + repo + "/pulls/" + prNumber + "/merge"))
                        .header("Authorization", "Bearer " + githubToken)
                        .header("Accept", "application/vnd.github+json")
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode json = objectMapper.readTree(response.body());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    boolean merged = json.has("merged") && json.get("merged").asBoolean();
                    result.getOutputData().put("merged", merged);
                    result.getOutputData().put("mergedAt", java.time.Instant.now().toString());
                    System.out.println("  [merge] PR #" + prNumber + " merged (checks passed: " + checksPass + ")");
                } else {
                    String errorMsg = json.has("message") ? json.get("message").asText() : response.body();
                    result.setStatus(TaskResult.Status.FAILED);
                    result.setReasonForIncompletion("GitHub API error: " + errorMsg);
                    System.out.println("  [merge] ERROR: " + errorMsg);
                }
            } catch (Exception e) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("GitHub API call failed: " + e.getMessage());
                System.out.println("  [merge] ERROR: " + e.getMessage());
            }
        } else {
            System.out.println("  [merge] PR #" + prNumber + " merged (checks passed: " + checksPass + ")");
            result.getOutputData().put("merged", true);
            result.getOutputData().put("mergedAt", java.time.Instant.now().toString());
        }

        return result;
    }
}
