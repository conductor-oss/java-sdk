package runbookautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Logs the runbook execution outcome to a real file on disk.
 * Writes a structured JSON log entry to /tmp/runbook-logs/ with
 * the runbook ID, verification status, duration, and timestamp.
 *
 * Input:
 *   - verified (boolean): whether verification passed
 *   - runbookId (String): optional runbook ID for correlation
 *   - totalDurationMs (long): optional execution duration
 *
 * Output:
 *   - outcome (String): "success" or "failure"
 *   - logFile (String): path to the log file
 *   - duration (long): total execution time in ms
 *   - loggedAt (String): ISO-8601 timestamp
 */
public class LogOutcomeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ra_log_outcome";
    }

    @Override
    public TaskResult execute(Task task) {
        boolean verified = Boolean.TRUE.equals(task.getInputData().get("verified"));
        String runbookId = task.getInputData().get("runbookId") != null
                ? String.valueOf(task.getInputData().get("runbookId")) : "unknown";
        long totalDurationMs = toLong(task.getInputData().get("totalDurationMs"), 0);

        String outcome = verified ? "success" : "failure";
        Instant now = Instant.now();

        System.out.println("[ra_log_outcome] Logging outcome: " + outcome + " for " + runbookId);

        TaskResult result = new TaskResult(task);

        try {
            // Write outcome to a real log file
            Path logDir = Path.of(System.getProperty("java.io.tmpdir"), "runbook-logs");
            Files.createDirectories(logDir);

            String logFileName = "runbook-" + runbookId + "-" + System.currentTimeMillis() + ".json";
            Path logFile = logDir.resolve(logFileName);

            String json = "{\n"
                    + "  \"runbookId\": \"" + runbookId + "\",\n"
                    + "  \"outcome\": \"" + outcome + "\",\n"
                    + "  \"verified\": " + verified + ",\n"
                    + "  \"durationMs\": " + totalDurationMs + ",\n"
                    + "  \"timestamp\": \"" + now + "\"\n"
                    + "}\n";

            Files.writeString(logFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Also append to a summary log
            Path summaryLog = logDir.resolve("summary.log");
            String summaryEntry = now + " | " + runbookId + " | " + outcome
                    + " | " + totalDurationMs + "ms\n";
            Files.writeString(summaryLog, summaryEntry,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            System.out.println("  Log file: " + logFile);
            System.out.println("  Outcome: " + outcome + " | Duration: " + totalDurationMs + "ms");

            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("outcome", outcome);
            result.addOutputData("logFile", logFile.toString());
            result.addOutputData("duration", totalDurationMs);
            result.addOutputData("loggedAt", now.toString());
            result.addOutputData("runbookId", runbookId);

        } catch (Exception e) {
            System.out.println("  Error writing log: " + e.getMessage());
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("outcome", outcome);
            result.addOutputData("duration", totalDurationMs);
            result.addOutputData("loggedAt", now.toString());
            result.addOutputData("error", e.getMessage());
        }

        return result;
    }

    private long toLong(Object val, long def) {
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return def; }
    }
}
