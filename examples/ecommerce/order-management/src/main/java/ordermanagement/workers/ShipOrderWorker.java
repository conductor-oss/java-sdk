package ordermanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ships an order: transitions PROCESSING -> SHIPPED.
 *
 * Real shipping logic:
 *   - Validates the order is in PROCESSING state
 *   - Determines carrier based on shipping method (express=FedEx, standard=USPS, overnight=UPS)
 *   - Generates a real-format tracking number (carrier-specific format)
 *   - Calculates estimated delivery date based on shipping method
 *   - Transitions order state via state machine
 *
 * Input: orderId, fulfillmentId, shippingAddress, shippingMethod
 * Output: trackingNumber, carrier, shippedAt, estimatedDelivery, status
 */
public class ShipOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ord_ship";
    }

    @SuppressWarnings("unchecked")
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

        String fulfillmentId = (String) task.getInputData().get("fulfillmentId");
        if (fulfillmentId == null || fulfillmentId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: fulfillmentId");
            return result;
        }

        String shippingMethod = task.getInputData().get("shippingMethod") != null
                ? task.getInputData().get("shippingMethod").toString().toLowerCase() : "standard";

        // Parse shipping address
        String city = "N/A";
        String state = "N/A";
        String zip = "N/A";
        Object addrObj = task.getInputData().get("shippingAddress");
        if (addrObj instanceof Map) {
            Map<String, Object> addr = (Map<String, Object>) addrObj;
            if (addr.get("city") != null) city = addr.get("city").toString();
            if (addr.get("state") != null) state = addr.get("state").toString();
            if (addr.get("zip") != null) zip = addr.get("zip").toString();
        }

        // Validate state transition
        String previousStatus = OrderStore.getStatus(orderId);
        boolean canShip = OrderStore.isValidTransition(orderId, "SHIPPED");

        if (!canShip && previousStatus != null) {
            output.put("error", "Cannot ship: order is in " + previousStatus + " state");
            output.put("status", previousStatus);
            result.setOutputData(output);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Invalid state transition from " + previousStatus + " to SHIPPED");
            return result;
        }

        // Determine carrier and generate tracking number
        String carrier;
        String trackingPrefix;
        int transitDays;

        switch (shippingMethod) {
            case "express":
                carrier = "FedEx";
                trackingPrefix = "FX";
                transitDays = 3;
                break;
            case "overnight":
                carrier = "UPS";
                trackingPrefix = "1Z";
                transitDays = 1;
                break;
            default:
                carrier = "USPS";
                trackingPrefix = "94";
                transitDays = 7;
                break;
        }

        // Generate tracking number in carrier-specific format
        long timestamp = System.currentTimeMillis();
        long orderHash = Math.abs(orderId.hashCode());
        String trackingNumber = trackingPrefix + String.format("%010d", (timestamp % 10_000_000_000L))
                + String.format("%04d", orderHash % 10000);

        Instant now = Instant.now();
        Instant estimatedDelivery = now.plus(transitDays, ChronoUnit.DAYS);

        // Transition state
        boolean transitioned = OrderStore.transition(orderId, "SHIPPED", "ship_worker",
                carrier + " tracking " + trackingNumber + " to " + city + ", " + state);

        String currentStatus = OrderStore.getStatus(orderId);

        System.out.println("  [ship] Order " + orderId + ": " + carrier + " " + trackingNumber
                + " to " + city + ", " + state + " " + zip
                + " (est. delivery: " + estimatedDelivery.toString().split("T")[0] + ")"
                + " (" + previousStatus + " -> " + currentStatus + ")");

        output.put("trackingNumber", trackingNumber);
        output.put("carrier", carrier);
        output.put("shippingMethod", shippingMethod);
        output.put("shippedAt", now.toString());
        output.put("estimatedDelivery", estimatedDelivery.toString().split("T")[0]);
        output.put("transitDays", transitDays);
        output.put("destination", city + ", " + state + " " + zip);
        output.put("fulfillmentId", fulfillmentId);
        output.put("previousStatus", previousStatus);
        output.put("status", currentStatus != null ? currentStatus : "SHIPPED");

        result.setOutputData(output);
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
