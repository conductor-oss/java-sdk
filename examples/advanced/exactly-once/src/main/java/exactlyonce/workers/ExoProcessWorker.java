package exactlyonce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Processes a message if not already processed. The result is deterministic
 * based on the messageId (SHA-256 hash) so re-processing yields the same output.
 *
 * If currentState is "completed", skips processing and returns alreadyProcessed=true.
 *
 * Input: messageId, payload, currentState
 * Output: processed, result (map with hash and payload summary)
 */
public class ExoProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "exo_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String messageId = (String) task.getInputData().get("messageId");
        if (messageId == null) messageId = "unknown";

        String currentState = (String) task.getInputData().get("currentState");
        Object payload = task.getInputData().get("payload");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if ("completed".equals(currentState)) {
            System.out.println("  [process] messageId=" + messageId + " already processed, skipping");
            result.getOutputData().put("processed", false);
            result.getOutputData().put("skipped", true);
            result.getOutputData().put("result", Map.of("reason", "already_processed"));
            return result;
        }

        // Compute a deterministic hash of the messageId for idempotent output
        String hash = computeSha256(messageId);
        String payloadSummary = payload != null ? payload.toString() : "null";
        if (payloadSummary.length() > 100) {
            payloadSummary = payloadSummary.substring(0, 100) + "...";
        }

        System.out.println("  [process] messageId=" + messageId + " processing payload, hash=" + hash.substring(0, 12));

        result.getOutputData().put("processed", true);
        result.getOutputData().put("result", Map.of(
                "hash", hash,
                "payloadSummary", payloadSummary,
                "messageId", messageId
        ));
        return result;
    }

    static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
