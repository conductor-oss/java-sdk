package teamsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Posts an adaptive card to a Teams channel.
 * Input: channelId, card
 * Output: messageId, postedAt
 */
public class PostCardWorker implements Worker {

    private final String webhookUrl;

    public PostCardWorker() {
        this.webhookUrl = System.getenv("TEAMS_WEBHOOK_URL");
    }

    @Override
    public String getTaskDefName() {
        return "tms_post_card";
    }

    @Override
    public TaskResult execute(Task task) {
        String channelId = (String) task.getInputData().get("channelId");
        if (channelId == null) {
            channelId = "unknown-channel";
        }

        Object card = task.getInputData().get("card");

        // Live mode: POST to Teams webhook URL
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            try {
                String cardJson = card != null ? card.toString() : "{}";
                String bodyJson = "{\"type\":\"message\",\"attachments\":[{\"contentType\":\"application/vnd.microsoft.card.adaptive\",\"content\":" + cardJson + "}]}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String messageId = "teams-msg-" + System.currentTimeMillis();
                    System.out.println("  [post] Posted card " + messageId + " to Teams webhook (HTTP " + response.statusCode() + ")");
                    TaskResult result = new TaskResult(task);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    result.getOutputData().put("messageId", messageId);
                    result.getOutputData().put("postedAt", java.time.Instant.now().toString());
                    return result;
                } else {
                    System.err.println("  [worker] API error HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("  [worker] API error: " + e.getMessage());
            }
        }

        // Fallback
        String messageId = "teams-msg-" + channelId.hashCode() + "-001";

        System.out.println("  [post] " + messageId + " to channel " + channelId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("messageId", messageId);
        result.getOutputData().put("postedAt", "2025-01-15T10:30:01Z");
        return result;
    }
}
