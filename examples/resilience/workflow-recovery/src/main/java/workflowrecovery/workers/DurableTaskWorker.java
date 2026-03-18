package workflowrecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Durable task worker with real checkpoint-based recovery.
 *
 * Maintains an in-memory checkpoint store. On each execution:
 * 1. Checks if this batch was already processed (checkpoint exists)
 * 2. If yes, resumes from checkpoint rather than reprocessing
 * 3. If no, processes the batch data and stores a checkpoint
 *
 * This demonstrates how Conductor's persistence + worker-side checkpointing
 * provides exactly-once semantics across server restarts.
 *
 * Input:  { batch: "batch-001" }
 * Output: { processed: true, batch: "batch-001", checkpointId, resumed, processedAt }
 */
public class DurableTaskWorker implements Worker {

    // Checkpoint store: batchId -> checkpointId
    static final ConcurrentHashMap<String, String> CHECKPOINTS = new ConcurrentHashMap<>();
    // Processing timestamp store: batchId -> ISO timestamp
    static final ConcurrentHashMap<String, String> TIMESTAMPS = new ConcurrentHashMap<>();

    @Override
    public String getTaskDefName() {
        return "wr_durable_task";
    }

    @Override
    public TaskResult execute(Task task) {
        String batch = "";
        Object batchInput = task.getInputData().get("batch");
        if (batchInput != null) {
            batch = batchInput.toString();
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        // Check for existing checkpoint (recovery scenario)
        String existingCheckpoint = CHECKPOINTS.get(batch);
        if (existingCheckpoint != null) {
            String originalTimestamp = TIMESTAMPS.getOrDefault(batch, Instant.now().toString());
            System.out.println("  [wr_durable_task] Resuming batch \"" + batch
                    + "\" from checkpoint " + existingCheckpoint);

            result.getOutputData().put("processed", true);
            result.getOutputData().put("batch", batch);
            result.getOutputData().put("checkpointId", existingCheckpoint);
            result.getOutputData().put("resumed", true);
            result.getOutputData().put("originalProcessedAt", originalTimestamp);
            result.getOutputData().put("processedAt", Instant.now().toString());
            return result;
        }

        // Process the batch and create a checkpoint
        String checkpointId = computeCheckpointId(batch);
        String processedAt = Instant.now().toString();

        CHECKPOINTS.put(batch, checkpointId);
        TIMESTAMPS.put(batch, processedAt);

        System.out.println("  [wr_durable_task] Processing batch \"" + batch
                + "\" -- checkpoint=" + checkpointId);

        result.getOutputData().put("processed", true);
        result.getOutputData().put("batch", batch);
        result.getOutputData().put("checkpointId", checkpointId);
        result.getOutputData().put("resumed", false);
        result.getOutputData().put("processedAt", processedAt);
        return result;
    }

    /**
     * Computes a deterministic checkpoint ID from the batch name.
     */
    private static String computeCheckpointId(String batch) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("checkpoint:" + batch).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder("chk-");
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            return "chk-" + batch.hashCode();
        }
    }

    /** Clear checkpoints for testing. */
    static void clearCheckpoints() {
        CHECKPOINTS.clear();
        TIMESTAMPS.clear();
    }
}
