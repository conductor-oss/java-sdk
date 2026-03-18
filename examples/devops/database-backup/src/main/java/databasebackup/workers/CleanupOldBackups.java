package databasebackup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Enforces the retention policy by identifying and removing old backups.
 * Lists existing backups from storage and applying the retention
 * rules: delete backups older than N days, and keep at most M backups total.
 *
 * Generates a realistic set of "existing" backups based on the database name
 * so the cleanup logic is exercised with deterministic data.
 */
public class CleanupOldBackups implements Worker {

    @Override
    public String getTaskDefName() {
        return "backup_cleanup_old";
    }

    @Override
    public TaskResult execute(Task task) {
        String databaseName = String.valueOf(task.getInputData().get("databaseName"));
        String storageType = String.valueOf(task.getInputData().get("storageType"));
        String bucket = String.valueOf(task.getInputData().get("bucket"));
        int retentionDays = toInt(task.getInputData().get("retentionDays"), 30);
        int maxBackups = toInt(task.getInputData().get("maxBackups"), 10);

        System.out.println("[backup_cleanup_old] Enforcing retention policy for '" + databaseName + "'...");
        System.out.println("  Policy: retain " + retentionDays + " days, max " + maxBackups + " backups");

        TaskResult result = new TaskResult(task);

        // Generate deterministic "existing" backups
        List<Map<String, Object>> existingBackups = generateExistingBackups(databaseName);

        System.out.println("  Found " + existingBackups.size() + " existing backups in storage");

        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        List<Map<String, Object>> toDelete = new ArrayList<>();
        List<Map<String, Object>> toKeep = new ArrayList<>();

        // Sort by timestamp descending (newest first)
        existingBackups.sort((a, b) -> {
            Instant ta = Instant.parse((String) a.get("timestamp"));
            Instant tb = Instant.parse((String) b.get("timestamp"));
            return tb.compareTo(ta);
        });

        for (int i = 0; i < existingBackups.size(); i++) {
            Map<String, Object> backup = existingBackups.get(i);
            Instant ts = Instant.parse((String) backup.get("timestamp"));

            if (ts.isBefore(cutoff)) {
                // Too old — delete
                toDelete.add(backup);
            } else if (toKeep.size() >= maxBackups) {
                // Over max count — delete
                toDelete.add(backup);
            } else {
                toKeep.add(backup);
            }
        }

        long freedBytes = 0;
        for (Map<String, Object> b : toDelete) {
            freedBytes += toLong(b.get("sizeBytes"), 0);
        }

        System.out.println("  Keeping: " + toKeep.size() + " backups");
        System.out.println("  Deleting: " + toDelete.size() + " backups");
        System.out.println("  Freed: " + TakeSnapshot.formatBytes(freedBytes));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("existingCount", existingBackups.size());
        result.getOutputData().put("deletedCount", toDelete.size());
        result.getOutputData().put("retainedCount", toKeep.size());
        result.getOutputData().put("freedBytes", freedBytes);
        result.getOutputData().put("freedFormatted", TakeSnapshot.formatBytes(freedBytes));
        result.getOutputData().put("retentionDays", retentionDays);
        result.getOutputData().put("maxBackups", maxBackups);
        result.getOutputData().put("deletedFiles", toDelete.stream()
                .map(b -> b.get("filename")).toList());
        return result;
    }

    /**
     * Generates a deterministic set of "existing" backups spanning the last 60 days.
     * The count is based on the database name hash so different databases produce
     * different backup histories.
     */
    static List<Map<String, Object>> generateExistingBackups(String databaseName) {
        int hash = Math.abs(databaseName.hashCode());
        int count = 5 + (hash % 15); // 5 to 19 existing backups
        List<Map<String, Object>> backups = new ArrayList<>();

        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
            // Spread backups over last 60 days, roughly daily
            int daysAgo = (i * 60) / count;
            Instant timestamp = now.minus(daysAgo, ChronoUnit.DAYS);
            long size = TakeSnapshot.computeDeterministicSize(databaseName + "_" + i);

            Map<String, Object> backup = new LinkedHashMap<>();
            backup.put("filename", databaseName + "_backup_" + i + ".sql.gz");
            backup.put("timestamp", timestamp.toString());
            backup.put("sizeBytes", size);
            backups.add(backup);
        }

        return backups;
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
