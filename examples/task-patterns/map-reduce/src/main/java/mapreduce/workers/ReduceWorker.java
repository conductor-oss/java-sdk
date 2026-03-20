package mapreduce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * REDUCE phase: aggregates results from all parallel log analysis tasks.
 *
 * Takes the joinOutput map (keyed by taskReferenceName) produced by the JOIN task
 * and fileCount. Collects results from log_N entries and calculates:
 * - totalErrors, totalWarnings, totalLines
 * - errorRate (percentage string like "0.500%")
 * - worstFile (file with most errors)
 * - healthStatus (CRITICAL if >100 errors, WARNING if >50, else HEALTHY)
 */
public class ReduceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mr_reduce";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> mapResults = (Map<String, Object>) task.getInputData().get("mapResults");
        Object fileCountObj = task.getInputData().get("fileCount");
        int fileCount = fileCountObj instanceof Number ? ((Number) fileCountObj).intValue() : 0;

        int totalErrors = 0;
        int totalWarnings = 0;
        int totalLines = 0;
        String worstFile = "none";
        int worstErrorCount = -1;
        int filesAnalyzed = 0;

        if (mapResults != null) {
            // Sort by key to get deterministic ordering (log_0_ref, log_1_ref, ...)
            TreeMap<String, Object> sorted = new TreeMap<>(mapResults);
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("log_") && key.endsWith("_ref")) {
                    Map<String, Object> taskOutput = (Map<String, Object>) entry.getValue();
                    if (taskOutput != null) {
                        filesAnalyzed++;

                        int errors = getInt(taskOutput, "errorCount");
                        int warnings = getInt(taskOutput, "warningCount");
                        int lines = getInt(taskOutput, "lineCount");

                        totalErrors += errors;
                        totalWarnings += warnings;
                        totalLines += lines;

                        if (errors > worstErrorCount) {
                            worstErrorCount = errors;
                            Object fn = taskOutput.get("fileName");
                            worstFile = fn != null ? fn.toString() : "unknown";
                        }
                    }
                }
            }
        }

        // Calculate error rate as percentage
        double errorRate = totalLines > 0 ? (double) totalErrors / totalLines * 100.0 : 0.0;
        String errorRateStr = String.format("%.3f%%", errorRate);

        // Determine health status
        String healthStatus;
        if (totalErrors > 100) {
            healthStatus = "CRITICAL";
        } else if (totalErrors > 50) {
            healthStatus = "WARNING";
        } else {
            healthStatus = "HEALTHY";
        }

        System.out.println("  [mr_reduce] Aggregated " + filesAnalyzed + " files: "
                + totalErrors + " errors, " + totalWarnings + " warnings, status=" + healthStatus);

        // Build report
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("filesAnalyzed", filesAnalyzed);
        report.put("totalErrors", totalErrors);
        report.put("totalWarnings", totalWarnings);
        report.put("totalLines", totalLines);
        report.put("errorRate", errorRateStr);
        report.put("worstFile", worstFile);
        report.put("healthStatus", healthStatus);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", report);
        return result;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }
}
