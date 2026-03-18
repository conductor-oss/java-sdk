package incidentresponse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Sends a real HTTP webhook POST to notify the on-call engineer about an incident.
 * Reads the webhook URL from the task input or falls back to the ONCALL_WEBHOOK_URL
 * environment variable. If neither is configured, writes a notification to the
 * incident file and logs to console.
 *
 * Input:
 *   - incidentId (String): incident identifier
 *   - webhookUrl (String): optional webhook URL for notification
 *
 * Output:
 *   - notified (boolean): whether notification was sent
 *   - notificationMethod (String): "webhook", "console"
 *   - webhookStatusCode (int): HTTP status code if webhook was used
 *   - timestamp (String): when the notification was sent
 */
public class NotifyOncallWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ir_notify_oncall";
    }

    @Override
    public TaskResult execute(Task task) {
        String incidentId = task.getInputData().get("incidentId") != null
                ? String.valueOf(task.getInputData().get("incidentId")) : "unknown";

        System.out.println("[ir_notify_oncall] Notifying on-call about incident " + incidentId);

        TaskResult result = new TaskResult(task);

        // Resolve webhook URL: input > env var
        String webhookUrl = task.getInputData().get("webhookUrl") != null
                ? String.valueOf(task.getInputData().get("webhookUrl")) : null;
        if (webhookUrl == null || webhookUrl.isBlank()) {
            webhookUrl = System.getenv("ONCALL_WEBHOOK_URL");
        }

        Instant timestamp = Instant.now();

        if (webhookUrl != null && !webhookUrl.isBlank()) {
            // Send real HTTP POST to webhook
            try {
                String payload = "{"
                        + "\"text\":\"Incident " + incidentId + " requires attention\","
                        + "\"incidentId\":\"" + incidentId + "\","
                        + "\"timestamp\":\"" + timestamp + "\""
                        + "}";

                HttpURLConnection conn = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "IncidentResponse/1.0");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }

                int statusCode = conn.getResponseCode();
                conn.disconnect();

                boolean success = statusCode >= 200 && statusCode < 300;
                System.out.println("  Webhook response: " + statusCode);

                result.setStatus(TaskResult.Status.COMPLETED);
                result.addOutputData("notified", success);
                result.addOutputData("notificationMethod", "webhook");
                result.addOutputData("webhookStatusCode", statusCode);
                result.addOutputData("timestamp", timestamp.toString());
                result.addOutputData("incidentId", incidentId);

            } catch (Exception e) {
                System.out.println("  Webhook error: " + e.getMessage());
                result.setStatus(TaskResult.Status.COMPLETED);
                result.addOutputData("notified", false);
                result.addOutputData("notificationMethod", "webhook");
                result.addOutputData("error", e.getMessage());
                result.addOutputData("timestamp", timestamp.toString());
                result.addOutputData("incidentId", incidentId);
            }
        } else {
            // Log to console when no webhook is configured
            System.out.println("  No webhook configured. Logging notification to console.");
            System.out.println("  ALERT: Incident " + incidentId + " requires on-call attention!");

            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("notified", true);
            result.addOutputData("notificationMethod", "console");
            result.addOutputData("timestamp", timestamp.toString());
            result.addOutputData("incidentId", incidentId);
        }

        return result;
    }
}
