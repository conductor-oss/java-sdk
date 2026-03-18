package incidentresponse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

/**
 * Creates a real incident record by writing a JSON file to the local filesystem.
 * Each incident gets a unique ID and is persisted in /tmp/incidents/ as a
 * structured JSON document containing the alert details, severity, timestamps,
 * and status.
 *
 * Input:
 *   - alertName (String): name/title of the alert
 *   - severity (String): incident severity (P1, P2, P3, P4)
 *
 * Output:
 *   - incidentId (String): unique incident identifier
 *   - incidentFile (String): path to the created JSON file
 *   - createdAt (String): ISO-8601 creation timestamp
 *   - severity (String): incident severity
 *   - status (String): initial status ("open")
 */
public class CreateIncidentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ir_create_incident";
    }

    @Override
    public TaskResult execute(Task task) {
        String alertName = task.getInputData().get("alertName") != null
                ? String.valueOf(task.getInputData().get("alertName")) : "Unknown Alert";
        String severity = task.getInputData().get("severity") != null
                ? String.valueOf(task.getInputData().get("severity")) : "P3";

        String incidentId = "INC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant createdAt = Instant.now();

        System.out.println("[ir_create_incident] Creating incident " + incidentId
                + " for '" + alertName + "' (severity: " + severity + ")");

        TaskResult result = new TaskResult(task);

        try {
            // Write incident to a real JSON file
            Path incidentDir = Path.of(System.getProperty("java.io.tmpdir"), "incidents");
            Files.createDirectories(incidentDir);

            Path incidentFile = incidentDir.resolve(incidentId + ".json");

            String json = "{\n"
                    + "  \"incidentId\": \"" + incidentId + "\",\n"
                    + "  \"alertName\": \"" + escapeJson(alertName) + "\",\n"
                    + "  \"severity\": \"" + severity + "\",\n"
                    + "  \"status\": \"open\",\n"
                    + "  \"createdAt\": \"" + createdAt + "\",\n"
                    + "  \"updatedAt\": \"" + createdAt + "\",\n"
                    + "  \"assignedTo\": null,\n"
                    + "  \"timeline\": [\n"
                    + "    {\n"
                    + "      \"action\": \"created\",\n"
                    + "      \"timestamp\": \"" + createdAt + "\",\n"
                    + "      \"detail\": \"Incident created from alert: " + escapeJson(alertName) + "\"\n"
                    + "    }\n"
                    + "  ]\n"
                    + "}\n";

            Files.writeString(incidentFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("  Incident file: " + incidentFile);
            System.out.println("  Severity: " + severity);

            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("incidentId", incidentId);
            result.addOutputData("incidentFile", incidentFile.toString());
            result.addOutputData("createdAt", createdAt.toString());
            result.addOutputData("severity", severity);
            result.addOutputData("status", "open");
            result.addOutputData("alertName", alertName);

        } catch (Exception e) {
            System.out.println("  Failed to create incident file: " + e.getMessage());
            // Still return the incident ID even if file write fails
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("incidentId", incidentId);
            result.addOutputData("createdAt", createdAt.toString());
            result.addOutputData("severity", severity);
            result.addOutputData("status", "open");
            result.addOutputData("error", e.getMessage());
        }

        return result;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
