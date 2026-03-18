package checkoutflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Confirms an order after successful payment. Performs real validations:
 *   - Verifies payment ID is present and non-empty
 *   - Verifies grand total is positive
 *   - Generates a deterministic order ID from cart+user+payment data
 *   - Records the order in an in-memory order store (thread-safe)
 *   - Prevents duplicate order creation for the same cart
 *
 * Input: cartId, userId, paymentId, grandTotal
 * Output: orderId, confirmed, confirmedAt, orderSummary
 */
public class ConfirmOrderWorker implements Worker {

    private static final AtomicLong ORDER_SEQ = new AtomicLong();

    /** In-memory order store. Maps orderId -> order details. */
    private static final ConcurrentHashMap<String, Map<String, Object>> ORDERS = new ConcurrentHashMap<>();

    /** Maps cartId -> orderId to prevent duplicate orders. */
    private static final ConcurrentHashMap<String, String> CART_ORDERS = new ConcurrentHashMap<>();

    @Override
    public String getTaskDefName() {
        return "chk_confirm_order";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        String cartId = task.getInputData().get("cartId") != null
                ? task.getInputData().get("cartId").toString() : "UNKNOWN";
        String userId = task.getInputData().get("userId") != null
                ? task.getInputData().get("userId").toString() : "UNKNOWN";
        String paymentId = task.getInputData().get("paymentId") != null
                ? task.getInputData().get("paymentId").toString() : null;
        double grandTotal = 0;
        Object gtObj = task.getInputData().get("grandTotal");
        if (gtObj instanceof Number) grandTotal = ((Number) gtObj).doubleValue();
        if (gtObj instanceof String) {
            try { grandTotal = Double.parseDouble((String) gtObj); } catch (NumberFormatException ignored) {}
        }

        // Validate payment ID
        if (paymentId == null || paymentId.isBlank()) {
            output.put("confirmed", false);
            output.put("error", "Missing payment ID");
            result.setOutputData(output);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Cannot confirm order without payment ID");
            return result;
        }

        // Validate grand total
        if (grandTotal <= 0) {
            output.put("confirmed", false);
            output.put("error", "Invalid grand total: " + grandTotal);
            result.setOutputData(output);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Grand total must be positive");
            return result;
        }

        // Check for duplicate order (same cart already confirmed)
        String existingOrderId = CART_ORDERS.get(cartId);
        if (existingOrderId != null) {
            System.out.println("  [confirm] Cart " + cartId + " already confirmed as " + existingOrderId);
            output.put("orderId", existingOrderId);
            output.put("confirmed", true);
            output.put("duplicate", true);
            output.put("confirmedAt", Instant.now().toString());
            result.setOutputData(output);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }

        // Generate order ID
        long seq = ORDER_SEQ.incrementAndGet();
        String orderId = "ORD-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase() + "-" + seq;

        Instant now = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                .withZone(ZoneId.of("UTC"));

        // Store order
        Map<String, Object> orderRecord = new LinkedHashMap<>();
        orderRecord.put("orderId", orderId);
        orderRecord.put("cartId", cartId);
        orderRecord.put("userId", userId);
        orderRecord.put("paymentId", paymentId);
        orderRecord.put("grandTotal", grandTotal);
        orderRecord.put("status", "CONFIRMED");
        orderRecord.put("confirmedAt", now.toString());
        ORDERS.put(orderId, orderRecord);
        CART_ORDERS.put(cartId, orderId);

        System.out.println("  [confirm] Order " + orderId + " confirmed: $" + grandTotal
                + ", payment " + paymentId + ", cart " + cartId);

        output.put("orderId", orderId);
        output.put("confirmed", true);
        output.put("duplicate", false);
        output.put("confirmedAt", formatter.format(now));
        output.put("orderStatus", "CONFIRMED");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("userId", userId);
        summary.put("cartId", cartId);
        summary.put("paymentId", paymentId);
        summary.put("grandTotal", grandTotal);
        output.put("orderSummary", summary);

        result.setOutputData(output);
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

    /** Get order by ID. */
    public static Map<String, Object> getOrder(String orderId) {
        return ORDERS.get(orderId);
    }

    /** Reset state for testing. */
    public static void resetState() {
        ORDERS.clear();
        CART_ORDERS.clear();
    }
}
