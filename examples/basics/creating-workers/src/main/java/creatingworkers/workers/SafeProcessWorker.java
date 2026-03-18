package creatingworkers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Worker that processes data records with error handling.
 *
 * Takes a list of records and processes each one, computing a deterministic
 * quality score based on the record's hash. Demonstrates how to handle errors
 * gracefully: return FAILED status and let Conductor retry automatically.
 */
public class SafeProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "safe_process";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        try {
            Object rawRecords = task.getInputData().get("records");
            if (rawRecords == null) {
                System.out.println("  [safe_process] No records to process.");
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("processedCount", 0);
                result.getOutputData().put("results", List.of());
                result.getOutputData().put("summary", "No records provided");
                return result;
            }

            List<Map<String, Object>> records = (List<Map<String, Object>>) rawRecords;
            System.out.println("  [safe_process] Processing " + records.size() + " records...");

            List<Map<String, Object>> processed = new ArrayList<>();
            int passCount = 0;
            int failCount = 0;

            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> record = records.get(i);

                // Deterministic score based on record hash and index
                int baseScore = Math.abs(record.hashCode()) % 100;
                int adjustedScore = (baseScore + (i * 17)) % 100;
                String quality = adjustedScore >= 50 ? "PASS" : "FAIL";

                if ("PASS".equals(quality)) {
                    passCount++;
                } else {
                    failCount++;
                }

                processed.add(Map.of(
                        "recordId", record.getOrDefault("id", i),
                        "score", adjustedScore,
                        "quality", quality,
                        "processed", true
                ));
            }

            System.out.println("  [safe_process] Done: " + passCount + " passed, " + failCount + " failed.");

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("processedCount", records.size());
            result.getOutputData().put("passCount", passCount);
            result.getOutputData().put("failCount", failCount);
            result.getOutputData().put("results", processed);
            result.getOutputData().put("summary",
                    "Processed " + records.size() + " records: " + passCount + " passed, " + failCount + " failed");
            return result;

        } catch (Exception e) {
            System.err.println("  [safe_process] ERROR: " + e.getMessage());

            // Return FAILED — Conductor will retry based on retryCount in the task definition
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Processing error: " + e.getMessage());
            result.getOutputData().put("error", e.getMessage());
            return result;
        }
    }
}
