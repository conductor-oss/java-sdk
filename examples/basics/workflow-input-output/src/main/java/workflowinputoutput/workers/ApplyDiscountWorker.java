package workflowinputoutput.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Applies a coupon discount to the subtotal.
 *
 * Input:  subtotal, couponCode
 * Output: couponCode, discountRate, discountAmount, discountedTotal
 */
public class ApplyDiscountWorker implements Worker {

    private static final Map<String, Double> COUPONS = Map.of(
            "SAVE10", 0.10,
            "SAVE20", 0.20,
            "HALF", 0.50
    );

    @Override
    public String getTaskDefName() {
        return "apply_discount";
    }

    @Override
    public TaskResult execute(Task task) {
        double subtotal = toDouble(task.getInputData().get("subtotal"));
        String couponCode = (String) task.getInputData().get("couponCode");

        double discountRate = 0.0;
        if (couponCode != null && !couponCode.isBlank()) {
            discountRate = COUPONS.getOrDefault(couponCode, 0.0);
        }

        double discountAmount = subtotal * discountRate;
        double discountedTotal = subtotal - discountAmount;

        System.out.printf("  [apply_discount] Coupon '%s' → %.0f%% off $%.2f = -$%.2f → $%.2f%n",
                couponCode != null ? couponCode : "none",
                discountRate * 100, subtotal, discountAmount, discountedTotal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("couponCode", couponCode != null ? couponCode : "");
        result.getOutputData().put("discountRate", discountRate);
        result.getOutputData().put("discountAmount", discountAmount);
        result.getOutputData().put("discountedTotal", discountedTotal);
        return result;
    }

    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
