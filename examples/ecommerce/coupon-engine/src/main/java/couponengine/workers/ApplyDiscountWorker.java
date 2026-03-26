package couponengine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

public class ApplyDiscountWorker implements Worker {
    @Override public String getTaskDefName() { return "cpn_apply_discount"; }

    @Override
    public TaskResult execute(Task task) {
        double cartTotal = toDouble(task.getInputData().get("cartTotal"));
        String discountType = (String) task.getInputData().getOrDefault("discountType", "percentage");
        double discountValue = toDouble(task.getInputData().get("discountValue"));
        double discountAmount;
        if ("percentage".equals(discountType)) {
            discountAmount = Math.round(cartTotal * discountValue / 100.0 * 100.0) / 100.0;
        } else {
            discountAmount = discountValue;
        }
        double newTotal = Math.round((cartTotal - discountAmount) * 100.0) / 100.0;

        System.out.printf("  [apply] Code \"%s\": -$%.2f (%.0f%% off $%.2f) -> $%.2f%n",
                task.getInputData().get("couponCode"), discountAmount, discountValue, cartTotal, newTotal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("discountAmount", discountAmount);
        output.put("newTotal", newTotal);
        output.put("appliedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }

    private double toDouble(Object o) { if (o instanceof Number) return ((Number)o).doubleValue(); try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; } }
}
