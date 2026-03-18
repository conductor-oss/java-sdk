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
 * Updates a status page by writing the current component statuses and
 * any incidents to a real JSON file on disk at /tmp/uptime-status-page/.
 * This file could be served by a web server as a status page API.
 */
public class UpdateStatusPage implements Worker {

    @Override
    public String getTaskDefName() {
        return "uptime_update_status_page";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[uptime_update_status_page] Updating status page...");

        TaskResult result = new TaskResult(task);
        List<Map<String, Object>> failures = (List<Map<String, Object>>) task.getInputData().get("failures");
        String overallStatus = String.valueOf(task.getInputData().get("overallStatus"));

        List<Map<String, Object>> components = new ArrayList<>();
        if (failures != null) {
            for (Map<String, Object> f : failures) {
                String epStatus = String.valueOf(f.get("status"));
                String componentStatus = "down".equals(epStatus) ? "major_outage" : "degraded_performance";
                components.add(Map.of(
                        "name", f.get("name"),
                        "status", componentStatus
                ));
            }
        }

        // Create incident if there are down endpoints
        Map<String, Object> incident = null;
        if (failures != null && !failures.isEmpty()) {
            String impact = "critical".equals(overallStatus) ? "major" : "minor";
            incident = Map.of(
                    "id", "inc-" + UUID.randomUUID().toString().substring(0, 8),
                    "title", "Endpoint degradation detected",
                    "impact", impact,
                    "createdAt", Instant.now().toString()
            );
        }

        // Write status page to a real JSON file
        try {
            Path statusDir = Path.of(System.getProperty("java.io.tmpdir"), "uptime-status-page");
            Files.createDirectories(statusDir);

            Path statusFile = statusDir.resolve("status.json");

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"overallStatus\": \"").append(overallStatus).append("\",\n");
            json.append("  \"updatedAt\": \"").append(Instant.now()).append("\",\n");
            json.append("  \"components\": [\n");
            for (int i = 0; i < components.size(); i++) {
                Map<String, Object> c = components.get(i);
                json.append("    {\"name\": \"").append(c.get("name"))
                        .append("\", \"status\": \"").append(c.get("status")).append("\"}");
                if (i < components.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]");
            if (incident != null) {
                json.append(",\n  \"incident\": {\n");
                json.append("    \"id\": \"").append(incident.get("id")).append("\",\n");
                json.append("    \"title\": \"").append(incident.get("title")).append("\",\n");
                json.append("    \"impact\": \"").append(incident.get("impact")).append("\",\n");
                json.append("    \"createdAt\": \"").append(incident.get("createdAt")).append("\"\n");
                json.append("  }");
            }
            json.append("\n}\n");

            Files.writeString(statusFile, json.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("  Status page: " + statusFile);
        } catch (Exception e) {
            System.out.println("  Failed to update status page: " + e.getMessage());
        }

        System.out.println("  Updated " + components.size() + " components");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "updated");
        result.getOutputData().put("components", components);
        if (incident != null) {
            result.getOutputData().put("incident", incident);
        }
        return result;
    }
}
