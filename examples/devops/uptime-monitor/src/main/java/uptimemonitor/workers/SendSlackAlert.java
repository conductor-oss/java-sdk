package uptimemonitor.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Sends Slack alert with failure details.
 * Uses webhook URL from workflow input (slackConfig.webhook), falls back to
 * SLACK_WEBHOOK_URL env var. If neither is set, logs the message to console.
 */
public class SendSlackAlert implements Worker {

    @Override
    public String getTaskDefName() {
        return "uptime_send_slack_alert";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[uptime_send_slack_alert] Sending Slack alert...");

        TaskResult result = new TaskResult(task);
        List<Map<String, Object>> failures = (List<Map<String, Object>>) task.getInputData().get("failures");
        Map<String, Object> summary = (Map<String, Object>) task.getInputData().get("summary");
        Map<String, Object> slackConfig = (Map<String, Object>) task.getInputData().get("slackConfig");

        // Resolve webhook URL: workflow input > env var
        String webhookUrl = null;
        String channel = "#alerts";
        if (slackConfig != null) {
            webhookUrl = (String) slackConfig.get("webhook");
            if (slackConfig.get("channel") != null) {
                channel = (String) slackConfig.get("channel");
            }
        }
        if (webhookUrl == null || webhookUrl.isBlank()) {
            webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        }

        // Build message
        StringBuilder msg = new StringBuilder();
        msg.append(":rotating_light: *Uptime Alert*\n");
        if (summary != null) {
            msg.append("Endpoints: ").append(summary.get("totalEndpoints"))
                    .append(" total, ").append(summary.get("down")).append(" down, ")
                    .append(summary.get("degraded")).append(" degraded\n");
        }
        if (failures != null) {
            for (Map<String, Object> f : failures) {
                msg.append("• *").append(f.get("name")).append("*: ")
                        .append(f.get("status")).append(" — ")
                        .append(f.get("failedChecks")).append("\n");
            }
        }

        String status;
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            try {
                String escaped = msg.toString()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
                String payload = "{\"text\":\"" + escaped + "\"}";
                HttpURLConnection conn = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                conn.disconnect();
                status = code < 400 ? "sent" : "webhook_error";
                System.out.println("  Slack webhook response: " + code);
            } catch (Exception e) {
                status = "webhook_error";
                System.out.println("  Slack webhook error: " + e.getMessage());
            }
        } else {
            status = "logged";
            System.out.println("  [fallback] No webhook configured — logging alert to console");
            System.out.println("  " + msg.toString().replace("\n", "\n  "));
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", status);
        result.getOutputData().put("channel", channel);
        result.getOutputData().put("failureCount", failures != null ? failures.size() : 0);
        return result;
    }
}
