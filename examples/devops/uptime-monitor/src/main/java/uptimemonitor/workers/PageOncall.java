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
 * Pages on-call engineer by writing a real incident record to disk.
 * Creates a paging record in /tmp/uptime-alerts/pagerduty/ with incident
 * details, urgency level, and assignment information.
 *
 * The on-call engineer is selected in a round-robin fashion based on
 * the current time to distribute paging load.
 *
 * In production, replace with PagerDuty/OpsGenie API calls.
 */
public class PageOncall implements Worker {

    private static final String[] ONCALL_ENGINEERS = {
            "alice", "bob", "carol", "dave", "eve"
    };

    @Override
    public String getTaskDefName() {
        return "uptime_page_oncall";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[uptime_page_oncall] Paging on-call engineer...");

        TaskResult result = new TaskResult(task);
        String overallStatus = String.valueOf(task.getInputData().get("overallStatus"));

        String urgency = "critical".equals(overallStatus) ? "high" : "low";
        // Deterministic assignment based on current hour
        int hourOfDay = java.time.LocalTime.now().getHour();
        String engineer = ONCALL_ENGINEERS[hourOfDay % ONCALL_ENGINEERS.length];
        String incidentId = "INC-" + System.currentTimeMillis();

        // Write paging record to a real file
        try {
            Path pageDir = Path.of(System.getProperty("java.io.tmpdir"), "uptime-alerts", "pagerduty");
            Files.createDirectories(pageDir);

            Path pageFile = pageDir.resolve(incidentId + ".json");
            String json = "{\n"
                    + "  \"incidentId\": \"" + incidentId + "\",\n"
                    + "  \"assignedTo\": \"" + engineer + "\",\n"
                    + "  \"email\": \"" + engineer + "@example.com\",\n"
                    + "  \"urgency\": \"" + urgency + "\",\n"
                    + "  \"overallStatus\": \"" + overallStatus + "\",\n"
                    + "  \"createdAt\": \"" + Instant.now() + "\"\n"
                    + "}\n";

            Files.writeString(pageFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("  Paging record: " + pageFile);
        } catch (Exception e) {
            System.out.println("  Failed to write paging record: " + e.getMessage());
        }

        System.out.println("  Incident: " + incidentId);
        System.out.println("  Assigned to: " + engineer + " (urgency: " + urgency + ")");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "paged");
        result.getOutputData().put("incidentId", incidentId);
        result.getOutputData().put("assignedTo", engineer);
        result.getOutputData().put("email", engineer + "@example.com");
        result.getOutputData().put("urgency", urgency);
        result.getOutputData().put("createdAt", Instant.now().toString());
        return result;
    }
}
