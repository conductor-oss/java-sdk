package uptimemonitor.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

/**
 * Sends email alert for endpoint failures. Writes the alert to a real
 * file on disk (/tmp/uptime-alerts/email/) to persist the notification.
 * In production, replace the file write with actual SMTP/SES email sending.
 */
public class SendEmailAlert implements Worker {

    @Override
    public String getTaskDefName() {
        return "uptime_send_email_alert";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[uptime_send_email_alert] Sending email alert...");

        TaskResult result = new TaskResult(task);
        List<Map<String, Object>> failures = (List<Map<String, Object>>) task.getInputData().get("failures");
        Map<String, Object> summary = (Map<String, Object>) task.getInputData().get("summary");
        Map<String, Object> emailConfig = (Map<String, Object>) task.getInputData().get("emailConfig");

        List<String> recipients = emailConfig != null
                ? (List<String>) emailConfig.get("recipients")
                : List.of("oncall@example.com");

        int downCount = summary != null ? toInt(summary.get("down"), 0) : 0;
        String severity = downCount >= 3 ? "critical" : downCount >= 1 ? "warning" : "info";

        String messageId = "msg-" + UUID.randomUUID().toString().substring(0, 8);

        // Build the email body
        StringBuilder body = new StringBuilder();
        body.append("Subject: Uptime Alert - ").append(severity.toUpperCase()).append("\n");
        body.append("To: ").append(String.join(", ", recipients)).append("\n");
        body.append("Message-ID: ").append(messageId).append("\n\n");
        if (summary != null) {
            body.append("Endpoints: ").append(summary.get("totalEndpoints"))
                    .append(" total, ").append(summary.get("down")).append(" down, ")
                    .append(summary.get("degraded")).append(" degraded\n\n");
        }
        if (failures != null) {
            for (Map<String, Object> f : failures) {
                body.append("- ").append(f.get("name")).append(": ")
                        .append(f.get("status")).append(" (").append(f.get("failedChecks")).append(")\n");
            }
        }

        // Write to a real file on disk
        try {
            Path alertDir = Path.of(System.getProperty("java.io.tmpdir"), "uptime-alerts", "email");
            Files.createDirectories(alertDir);
            Path alertFile = alertDir.resolve(messageId + ".eml");
            Files.writeString(alertFile, body.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("  Email alert written to: " + alertFile);
        } catch (Exception e) {
            System.out.println("  Failed to write email alert: " + e.getMessage());
        }

        System.out.println("  Email to: " + recipients);
        System.out.println("  Severity: " + severity + " | Failures: " + (failures != null ? failures.size() : 0));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "sent");
        result.getOutputData().put("messageId", messageId);
        result.getOutputData().put("recipients", recipients);
        result.getOutputData().put("severity", severity);
        result.getOutputData().put("failureCount", failures != null ? failures.size() : 0);
        return result;
    }

    private int toInt(Object val, int def) {
        if (val instanceof Number) return ((Number) val).intValue();
        return def;
    }
}
