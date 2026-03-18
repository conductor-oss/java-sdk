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
 * Records healthy status by writing a health record to disk and computing
 * a real uptime percentage from historical data. Reads previous health
 * records from /tmp/uptime-health/ to calculate running uptime.
 */
public class RecordHealthy implements Worker {

    @Override
    public String getTaskDefName() {
        return "uptime_record_healthy";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[uptime_record_healthy] All endpoints healthy!");

        TaskResult result = new TaskResult(task);
        Map<String, Object> summary = (Map<String, Object>) task.getInputData().get("summary");

        Instant now = Instant.now();

        // Write healthy record to disk
        long healthyCount = 0;
        long totalCount = 0;
        try {
            Path healthDir = Path.of(System.getProperty("java.io.tmpdir"), "uptime-health");
            Files.createDirectories(healthDir);

            // Write this healthy record
            String filename = "health-" + System.currentTimeMillis() + ".json";
            Path healthFile = healthDir.resolve(filename);
            String json = "{\n"
                    + "  \"status\": \"healthy\",\n"
                    + "  \"timestamp\": \"" + now + "\",\n"
                    + "  \"totalEndpoints\": " + (summary != null ? summary.get("totalEndpoints") : 0) + "\n"
                    + "}\n";
            Files.writeString(healthFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Count existing records to compute uptime percentage
            try (var stream = Files.list(healthDir)) {
                var files = stream.filter(p -> p.toString().endsWith(".json")).toList();
                totalCount = files.size();
                for (Path f : files) {
                    String content = Files.readString(f);
                    if (content.contains("\"healthy\"")) {
                        healthyCount++;
                    }
                }
            }

            System.out.println("  Health record: " + healthFile);
        } catch (Exception e) {
            System.out.println("  Failed to write health record: " + e.getMessage());
            healthyCount = 1;
            totalCount = 1;
        }

        // Compute real uptime percentage from historical records
        double uptime = totalCount > 0 ? (double) healthyCount / totalCount * 100.0 : 100.0;
        String uptimeStr = String.format("%.2f%%", uptime);

        System.out.println("  Endpoints: " + (summary != null ? summary.get("totalEndpoints") : "?"));
        System.out.println("  Uptime: " + uptimeStr + " (" + healthyCount + "/" + totalCount + " checks)");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "healthy");
        result.getOutputData().put("uptime", uptimeStr);
        result.getOutputData().put("recordedAt", now.toString());
        return result;
    }
}
