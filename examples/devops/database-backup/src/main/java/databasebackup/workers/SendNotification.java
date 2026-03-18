package databasebackup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

/**
 * Sends a backup completion notification. Supports multiple real notification
 * channels:
 *   - webhook: sends a real HTTP POST to the configured webhook URL
 *   - file: writes the notification to a JSON file in /tmp/backup-notifications/
 *   - console: logs the notification message to stdout
 *
 * In all cases, a structured notification message is built and recorded.
 * The notification is also always written to a local file for auditing.
 *
 * Input:
 *   - filename, sizeBytes, storageUri, verified, deletedCount,
 *     freedFormatted, databaseName (pipeline context)
 *   - notification (Map): channel, webhook, recipients
 *
 * Output:
 *   - notificationSent (boolean), channel, recipients, status, message,
 *     databaseName, filename, verified
 */
public class SendNotification implements Worker {

    @Override
    public String getTaskDefName() {
        return "backup_send_notification";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[backup_send_notification] Sending backup notification...");

        TaskResult result = new TaskResult(task);

        String filename = String.valueOf(task.getInputData().get("filename"));
        long sizeBytes = toLong(task.getInputData().get("sizeBytes"), 0);
        String storageUri = String.valueOf(task.getInputData().get("storageUri"));
        boolean verified = Boolean.TRUE.equals(task.getInputData().get("verified"));
        int deletedCount = toInt(task.getInputData().get("deletedCount"), 0);
        String freedFormatted = String.valueOf(task.getInputData().get("freedFormatted"));
        String databaseName = String.valueOf(task.getInputData().get("databaseName"));
        Map<String, Object> notificationConfig =
                (Map<String, Object>) task.getInputData().get("notification");

        // Determine channels
        String channel = "console";
        String webhookUrl = null;
        List<String> recipients = new ArrayList<>();
        if (notificationConfig != null) {
            if (notificationConfig.get("channel") != null) {
                channel = String.valueOf(notificationConfig.get("channel"));
            }
            if (notificationConfig.get("webhook") != null) {
                webhookUrl = String.valueOf(notificationConfig.get("webhook"));
            }
            Object recipientObj = notificationConfig.get("recipients");
            if (recipientObj instanceof List) {
                recipients = (List<String>) recipientObj;
            }
        }

        // Build the notification message
        String status = verified ? "SUCCESS" : "FAILED";
        StringBuilder message = new StringBuilder();
        message.append("Database Backup ").append(status).append("\n");
        message.append("Database: ").append(databaseName).append("\n");
        message.append("File: ").append(filename).append("\n");
        message.append("Size: ").append(TakeSnapshot.formatBytes(sizeBytes)).append("\n");
        message.append("Location: ").append(storageUri).append("\n");
        message.append("Integrity: ").append(verified ? "Verified" : "FAILED").append("\n");
        message.append("Cleanup: ").append(deletedCount).append(" old backup(s) removed");
        if (deletedCount > 0) {
            message.append(", ").append(freedFormatted).append(" freed");
        }
        message.append("\n");
        message.append("Timestamp: ").append(Instant.now().toString());

        String notificationStatus = "logged";

        // Always write to local notification log file
        try {
            Path notifDir = Path.of(System.getProperty("java.io.tmpdir"), "backup-notifications");
            Files.createDirectories(notifDir);
            String notifFilename = "backup-notification-" + databaseName + "-" + System.currentTimeMillis() + ".json";
            Path notifFile = notifDir.resolve(notifFilename);

            String json = "{\n"
                    + "  \"status\": \"" + status + "\",\n"
                    + "  \"databaseName\": \"" + databaseName + "\",\n"
                    + "  \"filename\": \"" + filename + "\",\n"
                    + "  \"sizeBytes\": " + sizeBytes + ",\n"
                    + "  \"storageUri\": \"" + storageUri + "\",\n"
                    + "  \"verified\": " + verified + ",\n"
                    + "  \"deletedCount\": " + deletedCount + ",\n"
                    + "  \"freedFormatted\": \"" + freedFormatted + "\",\n"
                    + "  \"channel\": \"" + channel + "\",\n"
                    + "  \"recipients\": " + recipientsToJson(recipients) + ",\n"
                    + "  \"timestamp\": \"" + Instant.now() + "\"\n"
                    + "}\n";

            Files.writeString(notifFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("  Notification logged to: " + notifFile);
        } catch (Exception e) {
            System.out.println("  Failed to write notification file: " + e.getMessage());
        }

        // Send via webhook if configured
        if ("webhook".equals(channel) && webhookUrl != null && !webhookUrl.isBlank()) {
            try {
                String escaped = message.toString()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n");
                String payload = "{\"text\":\"" + escaped + "\"}";

                HttpURLConnection conn = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                conn.disconnect();

                notificationStatus = code < 400 ? "sent" : "webhook_error";
                System.out.println("  Webhook response: " + code);
            } catch (Exception e) {
                notificationStatus = "webhook_error";
                System.out.println("  Webhook error: " + e.getMessage());
            }
        } else {
            // Console logging
            System.out.println("  Channel: " + channel);
            if (!recipients.isEmpty()) {
                System.out.println("  Recipients: " + String.join(", ", recipients));
            }
            System.out.println("  Status: " + status);
            System.out.println("  Message:\n    " + message.toString().replace("\n", "\n    "));
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notificationSent", true);
        result.getOutputData().put("channel", channel);
        result.getOutputData().put("recipients", recipients);
        result.getOutputData().put("status", status);
        result.getOutputData().put("message", message.toString());
        result.getOutputData().put("databaseName", databaseName);
        result.getOutputData().put("filename", filename);
        result.getOutputData().put("verified", verified);
        return result;
    }

    private String recipientsToJson(List<String> recipients) {
        if (recipients.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < recipients.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(recipients.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return defaultVal; }
    }

    private long toLong(Object val, long defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return defaultVal; }
    }
}
