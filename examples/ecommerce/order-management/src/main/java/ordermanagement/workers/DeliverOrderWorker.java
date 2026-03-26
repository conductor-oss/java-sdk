package ordermanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Delivers an order: transitions SHIPPED -> DELIVERED.
 *
 * Real delivery logic:
 *   - Validates the order is in SHIPPED state
 *   - Records delivery confirmation with timestamp
 *   - Generates a delivery signature (derived from tracking number)
 *   - Calculates total transit time from shipment to delivery
 *   - Transitions to terminal DELIVERED state
 *   - Returns full order history
 *
 * Input: orderId, trackingNumber
 * Output: delivered, deliveredAt, signedBy, transitTime, orderHistory, status
 */
public class DeliverOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ord_deliver";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        // --- Validate required inputs ---
        String orderId = (String) task.getInputData().get("orderId");
        if (orderId == null || orderId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: orderId");
            return result;
        }

        String trackingNumber = (String) task.getInputData().get("trackingNumber");
        if (trackingNumber == null || trackingNumber.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: trackingNumber");
            return result;
        }

        // Validate state transition
        String previousStatus = OrderStore.getStatus(orderId);
        boolean canDeliver = OrderStore.isValidTransition(orderId, "DELIVERED");

        if (!canDeliver && previousStatus != null) {
            output.put("error", "Cannot deliver: order is in " + previousStatus + " state");
            output.put("delivered", false);
            output.put("status", previousStatus);
            result.setOutputData(output);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Invalid state transition from " + previousStatus + " to DELIVERED");
            return result;
        }

        Instant now = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                .withZone(ZoneId.of("UTC"));

        // Generate delivery signature from tracking number
        int sigHash = Math.abs(trackingNumber.hashCode());
        String[] sigNames = {"Front Desk", "Mailroom", "Resident", "Neighbor", "Locker", "Doorstep"};
        String signedBy = sigNames[sigHash % sigNames.length];

        // Transition state
        boolean transitioned = OrderStore.transition(orderId, "DELIVERED", "deliver_worker",
                "Delivered, signed by " + signedBy + " (tracking: " + trackingNumber + ")");

        // Get full order history
        List<Map<String, Object>> history = OrderStore.getHistory(orderId);

        // Calculate transit time if we have history
        String transitTime = "N/A";
        if (history.size() >= 2) {
            try {
                for (Map<String, Object> entry : history) {
                    if ("SHIPPED".equals(entry.get("to"))) {
                        Instant shippedAt = Instant.parse((String) entry.get("timestamp"));
                        long hours = java.time.Duration.between(shippedAt, now).toHours();
                        long minutes = java.time.Duration.between(shippedAt, now).toMinutes() % 60;
                        transitTime = hours + "h " + minutes + "m";
                        break;
                    }
                }
            } catch (Exception ignored) {
                // If parsing fails, keep N/A
            }
        }

        String currentStatus = OrderStore.getStatus(orderId);

        System.out.println("  [deliver] Order " + orderId + ": delivered"
                + " (tracking: " + trackingNumber + ", signed by: " + signedBy + ")"
                + " (" + previousStatus + " -> " + currentStatus + ")");

        output.put("delivered", true);
        output.put("deliveredAt", formatter.format(now));
        output.put("deliveryTimestamp", now.toString());
        output.put("signedBy", signedBy);
        output.put("trackingNumber", trackingNumber);
        output.put("transitTime", transitTime);
        output.put("previousStatus", previousStatus);
        output.put("status", currentStatus != null ? currentStatus : "DELIVERED");
        output.put("orderHistory", history);

        result.setOutputData(output);
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
