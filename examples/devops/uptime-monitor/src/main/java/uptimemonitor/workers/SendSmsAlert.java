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
 * Sends SMS alerts for critical escalations. Writes alert records to a real
 * file on disk (/tmp/uptime-alerts/sms/) for auditing. Each SMS message
 * gets a unique SID and is persisted as a JSON file.
 *
 * In production, replace with Twilio/AWS SNS API calls.
 */
public class SendSmsAlert implements Worker {

    @Override
    public String getTaskDefName() {
        return "uptime_send_sms_alert";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[uptime_send_sms_alert] Sending SMS alerts...");

        TaskResult result = new TaskResult(task);
        List<Map<String, Object>> failures = (List<Map<String, Object>>) task.getInputData().get("failures");
        Map<String, Object> smsConfig = (Map<String, Object>) task.getInputData().get("smsConfig");

        List<String> phones = smsConfig != null
                ? (List<String>) smsConfig.get("phones")
                : List.of("+1555000111");

        // Build message
        StringBuilder names = new StringBuilder();
        if (failures != null) {
            for (int i = 0; i < failures.size(); i++) {
                if (i > 0) names.append(", ");
                names.append(failures.get(i).get("name"));
            }
        }
        String message = "CRITICAL: " + (failures != null ? failures.size() : 0)
                + " endpoint(s) down — " + names;

        // Write SMS records to real files on disk
        List<String> messageSids = new ArrayList<>();
        try {
            Path smsDir = Path.of(System.getProperty("java.io.tmpdir"), "uptime-alerts", "sms");
            Files.createDirectories(smsDir);

            for (String phone : phones) {
                String sid = "SM" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                messageSids.add(sid);

                String json = "{\n"
                        + "  \"sid\": \"" + sid + "\",\n"
                        + "  \"to\": \"" + phone + "\",\n"
                        + "  \"message\": \"" + message.replace("\"", "\\\"") + "\",\n"
                        + "  \"timestamp\": \"" + Instant.now() + "\"\n"
                        + "}\n";

                Path smsFile = smsDir.resolve(sid + ".json");
                Files.writeString(smsFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("  SMS to " + phone + " (SID: " + sid + ") -> " + smsFile);
            }
        } catch (Exception e) {
            System.out.println("  Failed to write SMS alert: " + e.getMessage());
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "sent");
        result.getOutputData().put("phones", phones);
        result.getOutputData().put("messageSids", messageSids);
        result.getOutputData().put("message", message);
        return result;
    }
}
