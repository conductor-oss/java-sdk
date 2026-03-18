package idempotentprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Processes a message that has not been seen before.
 * Outputs are deterministic — the resultHash is derived from the messageId so
 * repeated invocations with the same input always produce the same output.
 *
 * Input:  messageId, payload
 * Output: success, resultHash
 */
public class ProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "idp_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String msgId = (String) task.getInputData().get("messageId");
        Object payload = task.getInputData().get("payload");
        if (msgId == null) {
            msgId = "unknown";
        }

        String resultHash = computeHash(msgId);

        System.out.println("[idp_process] Processing message " + msgId + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("success", true);
        result.getOutputData().put("resultHash", resultHash);
        return result;
    }

    /**
     * Produces a deterministic SHA-256 hash of the messageId, prefixed with "sha256:".
     * Visible for testing.
     */
    static String computeHash(String messageId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(messageId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder("sha256:");
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on all JVMs
            throw new RuntimeException(e);
        }
    }
}
