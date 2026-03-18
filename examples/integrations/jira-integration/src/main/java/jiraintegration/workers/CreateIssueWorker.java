package jiraintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

/**
 * Creates a Jira issue via the Jira REST API.
 *
 * <p>Requires JIRA_URL and JIRA_API_TOKEN to be set. JIRA_EMAIL is optional
 * but recommended for Jira Cloud (used for Basic auth).
 *
 * Input: project, summary, description, assignee
 * Output: issueKey, createdAt
 */
public class CreateIssueWorker implements Worker {

    private final String jiraUrl;
    private final String jiraEmail;
    private final String jiraApiToken;

    public CreateIssueWorker() {
        this.jiraUrl = System.getenv("JIRA_URL");
        this.jiraEmail = System.getenv("JIRA_EMAIL");
        this.jiraApiToken = System.getenv("JIRA_API_TOKEN");
    }

    @Override
    public String getTaskDefName() {
        return "jra_create_issue";
    }

    @Override
    public TaskResult execute(Task task) {
        if (jiraUrl == null || jiraUrl.isBlank() || jiraApiToken == null || jiraApiToken.isBlank()) {
            throw new IllegalStateException(
                    "JIRA_URL and JIRA_API_TOKEN environment variables are required. "
                            + "Set them in your .env file or environment to use this worker.");
        }

        String project = (String) task.getInputData().get("project");
        if (project == null) {
            project = "DEFAULT";
        }
        String summary = (String) task.getInputData().get("summary");
        if (summary == null) {
            summary = "Untitled";
        }
        String description = (String) task.getInputData().get("description");
        if (description == null) {
            description = "";
        }

        try {
            String auth = Base64.getEncoder().encodeToString(
                    ((jiraEmail != null ? jiraEmail : "") + ":" + jiraApiToken).getBytes());
            String bodyJson = "{\"fields\":{\"project\":{\"key\":\"" + project + "\"},"
                    + "\"summary\":\"" + summary.replace("\"", "\\\"") + "\","
                    + "\"description\":\"" + description.replace("\"", "\\\"") + "\","
                    + "\"issuetype\":{\"name\":\"Task\"}}}";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jiraUrl.replaceAll("/$", "") + "/rest/api/3/issue"))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String responseBody = response.body();
                String issueKey = project + "-???";
                int keyIdx = responseBody.indexOf("\"key\":");
                if (keyIdx >= 0) {
                    int start = responseBody.indexOf("\"", keyIdx + 6) + 1;
                    int end = responseBody.indexOf("\"", start);
                    if (start > 0 && end > start) {
                        issueKey = responseBody.substring(start, end);
                    }
                }
                System.out.println("  [create] Created issue " + issueKey + ": " + summary);
                TaskResult result = new TaskResult(task);
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("issueKey", issueKey);
                result.getOutputData().put("createdAt", java.time.Instant.now().toString());
                return result;
            } else {
                TaskResult result = new TaskResult(task);
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion(
                        "Jira API error HTTP " + response.statusCode() + ": " + response.body());
                return result;
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Jira API call failed: " + e.getMessage());
            return result;
        }
    }
}
