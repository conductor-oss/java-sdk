package shoppingcart.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies discount/coupon codes to the cart subtotal.
 * Input: cartId, subtotal, couponCode
 * Output: finalTotal, discountApplied, discountAmount
 */
public class ApplyDiscountsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cart_apply_discounts";
    }

    @Override
    public TaskResult execute(Task task) {
        String cartId = (String) task.getInputData().get("cartId");
        if (cartId == null) cartId = "UNKNOWN";

        double subtotal = 0.0;
        Object subtotalObj = task.getInputData().get("subtotal");
        if (subtotalObj instanceof Number) subtotal = ((Number) subtotalObj).doubleValue();

        String couponCode = (String) task.getInputData().get("couponCode");

        double discountAmount = 0.0;
        boolean discountApplied = false;

        if (couponCode != null && !couponCode.isEmpty()) {
            // Perform different coupon codes
            if ("SAVE10".equalsIgnoreCase(couponCode)) {
                discountAmount = Math.round(subtotal * 0.10 * 100.0) / 100.0;
                discountApplied = true;
            } else if ("FLAT20".equalsIgnoreCase(couponCode)) {
                discountAmount = Math.min(20.0, subtotal);
                discountApplied = true;
            } else {
                // Perform a generic 5% discount for any recognized code
                discountAmount = Math.round(subtotal * 0.05 * 100.0) / 100.0;
                discountApplied = true;
            }
        }

        double finalTotal = Math.round((subtotal - discountAmount) * 100.0) / 100.0;

        System.out.println("  [discount] Cart " + cartId + ": coupon=" + couponCode
                + " discount=$" + discountAmount + " final=$" + finalTotal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("finalTotal", finalTotal);
        output.put("discountApplied", discountApplied);
        output.put("discountAmount", discountAmount);
        output.put("couponCode", couponCode);
        result.setOutputData(output);
        return result;
    }
}
