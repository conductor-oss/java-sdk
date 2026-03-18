package choreographyvsorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public class ShipOrderWorker implements Worker {

    // Estimated delivery days by warehouse region
    private static final Map<String, Integer> DELIVERY_DAYS = Map.of(
            "WH-EAST", 3,
            "WH-WEST", 4,
            "WH-CENTRAL", 2
    );

    @Override
    public String getTaskDefName() {
        return "cvo_ship_order";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String orderId = (String) task.getInputData().getOrDefault("orderId", "unknown");
        String warehouse = (String) task.getInputData().getOrDefault("warehouse", "unknown");
        String customerId = (String) task.getInputData().getOrDefault("customerId", "unknown");

        if ("unknown".equals(orderId) || orderId == null || orderId.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Missing orderId — cannot ship without a valid order reference");
            return r;
        }

        if ("unknown".equals(warehouse) || warehouse == null || warehouse.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Missing warehouse — cannot ship without a source warehouse");
            return r;
        }

        // Generate a unique tracking number
        String trackingId = "TRACK-" + orderId.replaceAll("[^A-Za-z0-9]", "")
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Calculate estimated delivery based on warehouse
        int deliveryDays = DELIVERY_DAYS.getOrDefault(warehouse, 5);
        Instant shippedAt = Instant.now();
        Instant estimatedDelivery = shippedAt.plus(deliveryDays, ChronoUnit.DAYS);

        String carrier;
        if (deliveryDays <= 2) {
            carrier = "express";
        } else if (deliveryDays <= 3) {
            carrier = "standard";
        } else {
            carrier = "economy";
        }

        System.out.println("  [ship] Order " + orderId + " shipped from " + warehouse
                + " via " + carrier + " (tracking=" + trackingId
                + ", ETA=" + estimatedDelivery.toString().substring(0, 10) + ")");

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("shipped", true);
        r.getOutputData().put("trackingId", trackingId);
        r.getOutputData().put("orderId", orderId);
        r.getOutputData().put("warehouse", warehouse);
        r.getOutputData().put("carrier", carrier);
        r.getOutputData().put("shippedAt", shippedAt.toString());
        r.getOutputData().put("estimatedDelivery", estimatedDelivery.toString());
        r.getOutputData().put("deliveryDays", deliveryDays);
        return r;
    }
}
