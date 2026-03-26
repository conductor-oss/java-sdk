package slackintegration.workers;

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
 * Posts a message to a Slack channel.
 * Input: channel, message
 * Output: messageId, postedAt
 *
 * Runs in live mode when SLACK_BOT_TOKEN is set,
 * otherwise falls back to fallback mode.
 */
public class PostMessageWorker implements Worker {

    private final boolean liveMode;
    private final String botToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PostMessageWorker() {
        this.botToken = System.getenv("SLACK_BOT_TOKEN");
        this.liveMode = botToken != null && !botToken.isBlank();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "slk_post_message";
    }

    @Override
    public TaskResult execute(Task task) {
        String channel = (String) task.getInputData().get("channel");
        if (channel == null) {
            channel = "general";
        }
        String message = (String) task.getInputData().get("message");
        if (message == null) {
            message = "";
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (liveMode) {
            try {
                String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                        "channel", channel,
                        "text", message
                ));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://slack.com/api/chat.postMessage"))
                        .header("Authorization", "Bearer " + botToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode json = objectMapper.readTree(response.body());

                if (json.has("ok") && json.get("ok").asBoolean()) {
                    String ts = json.get("ts").asText();
                    String messageId = "msg-" + ts;
                    result.getOutputData().put("messageId", messageId);
                    result.getOutputData().put("postedAt", java.time.Instant.now().toString());
                    System.out.println("  [post] Posted " + messageId + " to #" + channel);
                } else {
                    String errorMsg = json.has("error") ? json.get("error").asText() : response.body();
                    result.setStatus(TaskResult.Status.FAILED);
                    result.setReasonForIncompletion("Slack API error: " + errorMsg);
                    System.out.println("  [post] ERROR: " + errorMsg);
                }
            } catch (Exception e) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("Slack API call failed: " + e.getMessage());
                System.out.println("  [post] ERROR: " + e.getMessage());
            }
        } else {
            String messageId = "msg-" + channel.hashCode() + "-001";
            System.out.println("  [post] Posted " + messageId + " to #" + channel);
            result.getOutputData().put("messageId", messageId);
            result.getOutputData().put("postedAt", java.time.Instant.now().toString());
        }

        return result;
    }
}
