package runbookautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a runbook definition from the filesystem. Looks for runbook JSON/YAML files
 * in /tmp/runbooks/ directory. If the runbook file does not exist, generates a
 * default runbook with common diagnostic steps and writes it to disk.
 *
 * Each runbook contains a list of steps with shell commands to execute.
 *
 * Input:
 *   - runbookName (String): name of the runbook to load
 *
 * Output:
 *   - runbookId (String): unique runbook identifier
 *   - runbookName (String): name of the loaded runbook
 *   - version (int): runbook version
 *   - steps (List): list of steps with commands
 *   - runbookPath (String): path to the runbook file
 */
public class LoadRunbookWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ra_load_runbook";
    }

    @Override
    public TaskResult execute(Task task) {
        String runbookName = task.getInputData().get("runbookName") != null
                ? String.valueOf(task.getInputData().get("runbookName")) : "default-diagnostics";

        System.out.println("[ra_load_runbook] Loading runbook: " + runbookName);

        TaskResult result = new TaskResult(task);

        try {
            Path runbookDir = Path.of(System.getProperty("java.io.tmpdir"), "runbooks");
            Files.createDirectories(runbookDir);

            Path runbookFile = runbookDir.resolve(runbookName + ".json");
            String runbookId = "RB-" + Math.abs(runbookName.hashCode() % 90000 + 10000);

            List<Map<String, Object>> steps;
            int version;

            if (Files.exists(runbookFile)) {
                // Load existing runbook
                String content = Files.readString(runbookFile);
                System.out.println("  Loaded runbook from: " + runbookFile);
                System.out.println("  Content length: " + content.length() + " bytes");

                // Parse the steps from the file (simplified - real impl would use JSON parser)
                steps = getDefaultSteps(runbookName);
                version = 2;
            } else {
                // Generate and persist a default runbook
                steps = getDefaultSteps(runbookName);
                version = 1;

                StringBuilder json = new StringBuilder();
                json.append("{\n");
                json.append("  \"runbookId\": \"").append(runbookId).append("\",\n");
                json.append("  \"name\": \"").append(runbookName).append("\",\n");
                json.append("  \"version\": ").append(version).append(",\n");
                json.append("  \"createdAt\": \"").append(Instant.now()).append("\",\n");
                json.append("  \"steps\": [\n");
                for (int i = 0; i < steps.size(); i++) {
                    Map<String, Object> step = steps.get(i);
                    json.append("    {\"name\": \"").append(step.get("name"))
                            .append("\", \"command\": \"").append(step.get("command"))
                            .append("\", \"timeoutSeconds\": ").append(step.get("timeoutSeconds"))
                            .append("}");
                    if (i < steps.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("  ]\n}\n");

                Files.writeString(runbookFile, json.toString());
                System.out.println("  Created default runbook at: " + runbookFile);
            }

            System.out.println("  Runbook: " + runbookName + " (v" + version + ", "
                    + steps.size() + " steps)");

            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("runbookId", runbookId);
            result.addOutputData("runbookName", runbookName);
            result.addOutputData("version", version);
            result.addOutputData("steps", steps);
            result.addOutputData("stepCount", steps.size());
            result.addOutputData("runbookPath", runbookFile.toString());

        } catch (Exception e) {
            System.out.println("  Error loading runbook: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Failed to load runbook: " + e.getMessage());
        }

        return result;
    }

    /**
     * Returns default diagnostic steps based on the runbook name.
     */
    static List<Map<String, Object>> getDefaultSteps(String runbookName) {
        List<Map<String, Object>> steps = new ArrayList<>();

        if (runbookName.contains("database") || runbookName.contains("db")) {
            steps.add(step("check-disk-space", "df -h", 10));
            steps.add(step("check-connections", "netstat -an | grep -c ESTABLISHED", 10));
            steps.add(step("check-processes", "ps aux | head -20", 10));
        } else if (runbookName.contains("network")) {
            steps.add(step("check-dns", "nslookup google.com", 10));
            steps.add(step("check-connectivity", "ping -c 3 8.8.8.8", 15));
            steps.add(step("check-routes", "netstat -rn", 10));
        } else {
            // Default diagnostic runbook
            steps.add(step("check-uptime", "uptime", 5));
            steps.add(step("check-disk", "df -h", 10));
            steps.add(step("check-memory", "vm_stat || free -h", 10));
            steps.add(step("check-processes", "ps aux | head -15", 10));
        }

        return steps;
    }

    private static Map<String, Object> step(String name, String command, int timeoutSeconds) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("name", name);
        s.put("command", command);
        s.put("timeoutSeconds", timeoutSeconds);
        return s;
    }
}
