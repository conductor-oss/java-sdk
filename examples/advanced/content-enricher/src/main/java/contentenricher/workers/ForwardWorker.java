package contentenricher.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Forwards the enriched message by computing a content hash (fingerprint)
 * of the enriched message and determining a routing destination based
 * on the customerId hash.
 *
 * Input: enrichedMessage
 * Output: forwarded (boolean), destination (String), contentHash (String)
 */
public class ForwardWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "enr_forward";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object msgObj = task.getInputData().get("enrichedMessage");
        Map<String, Object> msg = (msgObj instanceof Map) ? (Map<String, Object>) msgObj : Map.of();

        String customerId = msg.containsKey("customerId") ? String.valueOf(msg.get("customerId")) : "unknown";

        // Compute a SHA-256 content hash of the enriched message for deduplication
        String contentHash = computeHash(msg.toString());

        // Determine routing destination based on customer ID hash modulo
        String destination = routeByCustomer(customerId);

        System.out.println("  [forward] Forwarding enriched message for customer \"" + customerId
                + "\" to " + destination + " (hash: " + contentHash.substring(0, 12) + "...)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("forwarded", true);
        result.getOutputData().put("destination", destination);
        result.getOutputData().put("contentHash", contentHash);
        return result;
    }

    /**
     * Routes the message to a queue partition based on the hash of the customerId.
     * Visible for testing.
     */
    static String routeByCustomer(String customerId) {
        int hash = Math.abs(customerId.hashCode());
        int partition = hash % 4;
        return "order_processing_queue_p" + partition;
    }

    private static String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
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
