package shoppingcart.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Reserves inventory for all items in the cart.
 * Input: cartId, items (list of cart items)
 * Output: allReserved, reservations, reservationExpiry
 */
public class ReserveInventoryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cart_reserve_inventory";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String cartId = (String) task.getInputData().get("cartId");
        if (cartId == null) cartId = "UNKNOWN";
        List<Map<String, Object>> items = (List<Map<String, Object>>) task.getInputData().get("items");
        if (items == null) items = List.of();

        List<Map<String, Object>> reservations = new ArrayList<>();
        boolean allReserved = true;

        for (Map<String, Object> item : items) {
            String lineId = (String) item.getOrDefault("lineId", "unknown");
            String sku = (String) item.getOrDefault("sku", "unknown");
            int qty = 1;
            Object qtyObj = item.get("quantity");
            if (qtyObj instanceof Number) qty = ((Number) qtyObj).intValue();

            Map<String, Object> reservation = new LinkedHashMap<>();
            reservation.put("lineId", lineId);
            reservation.put("sku", sku);
            reservation.put("quantityReserved", qty);
            reservation.put("reserved", true);
            reservations.add(reservation);
        }

        String expiry = Instant.now().plus(30, ChronoUnit.MINUTES).toString();

        System.out.println("  [reserve] Cart " + cartId + ": reserved " + reservations.size()
                + " items, allReserved=" + allReserved);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("allReserved", allReserved);
        output.put("reservations", reservations);
        output.put("reservationExpiry", expiry);
        result.setOutputData(output);
        return result;
    }
}
