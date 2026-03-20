package sendgridintegration.workers;

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
 * Sends an email via SendGrid.
 * Input: to, from, subject, htmlBody
 * Output: messageId, delivered, sentAt
 *
 * Runs in live mode when SENDGRID_API_KEY is set,
 * otherwise falls back to fallback mode.
 */
public class SendEmailWorker implements Worker {

    private final boolean liveMode;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SendEmailWorker() {
        this.apiKey = System.getenv("SENDGRID_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "sgd_send_email";
    }

    @Override
    public TaskResult execute(Task task) {
        String to = (String) task.getInputData().get("to");
        String subject = (String) task.getInputData().get("subject");
        String htmlBody = (String) task.getInputData().get("htmlBody");
        String from = (String) task.getInputData().get("from");
        if (from == null || from.isBlank()) {
            from = "noreply@example.com";
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (liveMode) {
            try {
                String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                        "personalizations", java.util.List.of(java.util.Map.of(
                                "to", java.util.List.of(java.util.Map.of("email", to))
                        )),
                        "from", java.util.Map.of("email", from),
                        "subject", subject,
                        "content", java.util.List.of(java.util.Map.of(
                                "type", "text/html",
                                "value", htmlBody
                        ))
                ));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    // SendGrid returns 202 with X-Message-Id header on success
                    String messageId = response.headers().firstValue("X-Message-Id")
                            .orElse("sg-" + Long.toString(System.currentTimeMillis(), 36));
                    result.getOutputData().put("messageId", messageId);
                    result.getOutputData().put("delivered", true);
                    result.getOutputData().put("sentAt", java.time.Instant.now().toString());
                    System.out.println("  [send] Email " + messageId + " sent to " + to + ": \"" + subject + "\"");
                } else {
                    String errorMsg = response.body();
                    try {
                        JsonNode json = objectMapper.readTree(response.body());
                        if (json.has("errors")) {
                            errorMsg = json.get("errors").get(0).get("message").asText();
                        }
                    } catch (Exception ignored) {
                    }
                    result.setStatus(TaskResult.Status.FAILED);
                    result.setReasonForIncompletion("SendGrid API error: " + errorMsg);
                    System.out.println("  [send] ERROR: " + errorMsg);
                }
            } catch (Exception e) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("SendGrid API call failed: " + e.getMessage());
                System.out.println("  [send] ERROR: " + e.getMessage());
            }
        } else {
            String messageId = "sg-" + Long.toString(System.currentTimeMillis(), 36);
            System.out.println("  [send] Email " + messageId + " sent to " + to + ": \"" + subject + "\"");
            result.getOutputData().put("messageId", messageId);
            result.getOutputData().put("delivered", true);
            result.getOutputData().put("sentAt", java.time.Instant.now().toString());
        }

        return result;
    }
}
